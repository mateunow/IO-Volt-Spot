package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.client.OCMClient;
import pl.voltspot.backend.dto.external.ExternalOCMStation;
import pl.voltspot.backend.dto.station.ConnectorRequest;
import pl.voltspot.backend.dto.station.CreateStationRequest;
import pl.voltspot.backend.dto.station.StationDetailsResponse;
import pl.voltspot.backend.dto.station.StationMarkerResponse;
import pl.voltspot.backend.dto.station.StationStatusSnapshotResponse;
import pl.voltspot.backend.dto.station.UpdateStationRequest;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationConnector;
import pl.voltspot.backend.entity.StationOwner;
import pl.voltspot.backend.entity.StationStatusSnapshot;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.ForbiddenException;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.mapper.StationMapper;
import pl.voltspot.backend.repository.StationOwnerRepository;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.StationStatusSnapshotRepository;
import pl.voltspot.backend.repository.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Transactional
public class StationService {

    private static final Logger log = LoggerFactory.getLogger(StationService.class);

    public static final String MANUAL_SOURCE = "MANUAL";

    private final StationRepository stationRepository;
    private final StationStatusSnapshotRepository snapshotRepository;
    private final StationOwnerRepository stationOwnerRepository;
    private final UserRepository userRepository;
    private final OCMClient ocmClient;

    public List<StationMarkerResponse> getStations(Double minLat, Double maxLat, Double minLon, Double maxLon) {
        List<Station> stations;

        boolean noFilters = minLat == null && maxLat == null && minLon == null && maxLon == null;
        boolean allFilters = minLat != null && maxLat != null && minLon != null && maxLon != null;

        if (!noFilters && !allFilters) {
            throw new BadRequestException("Podaj wszystkie parametry bbox: minLat, maxLat, minLon, maxLon – albo żaden");
        }

        if (noFilters) {
            stations = stationRepository.findByActiveTrueOrderByIdAsc();
        } else {
            stations = stationRepository.findByActiveTrueAndLatitudeBetweenAndLongitudeBetweenOrderByIdAsc(
                    minLat, maxLat, minLon, maxLon
            );
        }

        return toMarkerResponsesWithStatus(stations);
    }

    private List<StationMarkerResponse> toMarkerResponsesWithStatus(List<Station> stations) {
        if (stations.isEmpty()) return List.of();

        List<Long> ids = stations.stream().map(Station::getId).toList();

        Map<Long, StationStatusSnapshot> latestByStation = snapshotRepository
                .findLatestForStations(ids)
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getStation().getId(),
                        s -> s,
                        (a, b) -> a
                ));

        return stations.stream()
                .map(station -> StationMapper.toMarkerResponse(
                        station,
                        latestByStation.get(station.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public StationDetailsResponse getStationById(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono stacji o id " + stationId));

        StationStatusSnapshot latestStatus = snapshotRepository
                .findTopByStationIdOrderByRecordedAtDesc(stationId)
                .orElse(null);
        return StationMapper.toDetailsResponse(station, latestStatus);
    }

    @Transactional(readOnly = true)
    public StationStatusSnapshotResponse getLatestStatus(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new NotFoundException("Nie znaleziono stacji o id " + stationId);
        }

        StationStatusSnapshot snapshot = snapshotRepository
                .findTopByStationIdOrderByRecordedAtDesc(stationId)
                .orElseThrow(() -> new NotFoundException(
                        "Brak snapshotu statusu dla stacji o id " + stationId));

        return StationMapper.toStatusResponse(snapshot);
    }

    @Transactional
    public StationDetailsResponse createStation(CreateStationRequest request, Long creatorUserId) {
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika o id " + creatorUserId));

        if (creator.getRole() != UserRole.OWNER && creator.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Tylko użytkownik z rolą OWNER albo ADMIN może dodać stację");
        }

        Station station = new Station();
        station.setExternalSource(MANUAL_SOURCE);
        station.setExternalId(UUID.randomUUID().toString());
        station.setName(request.name().trim());
        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
        station.setAddressLine(trimOrNull(request.addressLine()));
        station.setCity(trimOrNull(request.city()));
        station.setCountry(trimOrNull(request.country()));
        station.setOperatorName(trimOrNull(request.operatorName()));
        station.setOpeningHours(trimOrNull(request.openingHours()));
        station.setAccessType(trimOrNull(request.accessType()));
        station.setActive(true);
        Instant now = Instant.now();
        station.setLastSyncedAt(now);
        // Blokujemy nadpisanie statusu active przez import OCM (nie dotyczy MANUAL, ale spójność z update'em).
        station.setAdminActiveLockedUntil(now.plus(24, java.time.temporal.ChronoUnit.HOURS));

        applyConnectorsFromRequest(station, request.connectors());

        Station savedStation = stationRepository.save(station);

        int totalQuantity = totalConnectorQuantity(savedStation);
        StationStatusSnapshot snapshot = new StationStatusSnapshot();
        snapshot.setStation(savedStation);
        snapshot.setSource(MANUAL_SOURCE);
        snapshot.setAvailableCount(totalQuantity);
        snapshot.setOccupiedCount(0);
        snapshot.setReservedCount(0);
        snapshot.setOutOfServiceCount(0);
        snapshot.setUnknownCount(0);
        snapshot.setRecordedAt(now);
        StationStatusSnapshot savedSnapshot = snapshotRepository.save(snapshot);

        StationOwner ownership = new StationOwner();
        ownership.setStation(savedStation);
        ownership.setOwner(creator);
        stationOwnerRepository.save(ownership);
        savedStation.getOwners().add(ownership);

        log.info("Stacja utworzona ręcznie: id={}, owner={}, connectors={}",
                savedStation.getId(), creator.getId(), savedStation.getConnectors().size());

        return StationMapper.toDetailsResponse(savedStation, savedSnapshot);
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void applyConnectorsFromRequest(Station station, List<ConnectorRequest> requests) {
        station.getConnectors().clear();
        if (requests == null || requests.isEmpty()) return;

        for (ConnectorRequest req : requests) {
            StationConnector connector = new StationConnector();
            connector.setStation(station);
            connector.setConnectorType(req.connectorType().trim());
            connector.setCurrentType(trimOrNull(req.currentType()));
            connector.setPowerKw(req.powerKw());
            connector.setQuantity(req.quantity() != null && req.quantity() > 0 ? req.quantity() : 1);
            station.getConnectors().add(connector);
        }
    }

    private static int totalConnectorQuantity(Station station) {
        return station.getConnectors().stream()
                .mapToInt(c -> c.getQuantity() != null ? c.getQuantity() : 1)
                .sum();
    }

    /**
     * Sprawdza, czy aktualny użytkownik może modyfikować daną stację.
     * ADMIN może zawsze; OWNER tylko swoje stacje (wpis w station_owners).
     */
    public void requireWriteAccess(Long stationId, Long userId, UserRole userRole) {
        if (userRole == UserRole.ADMIN) {
            return;
        }
        if (userRole == UserRole.OWNER && stationOwnerRepository.existsByStationIdAndOwner_Id(stationId, userId)) {
            return;
        }
        throw new ForbiddenException("Brak uprawnień do modyfikacji tej stacji");
    }

    @Transactional
    public StationDetailsResponse updateStation(Long stationId, UpdateStationRequest request) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono stacji o id " + stationId));

        station.setName(request.name());
        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
        station.setAddressLine(request.addressLine());
        station.setCity(request.city());
        station.setCountry(request.country());
        station.setOperatorName(request.operatorName());
        station.setOpeningHours(request.openingHours());
        station.setAccessType(request.accessType());
        station.setActive(request.active());
        station.setAdminActiveLockedUntil(Instant.now().plus(24, java.time.temporal.ChronoUnit.HOURS));
        station.setLastSyncedAt(Instant.now());

        if (request.connectors() != null) {
            applyConnectorsFromRequest(station, request.connectors());
        }

        Station savedStation = stationRepository.save(station);
        StationStatusSnapshot latestStatus = snapshotRepository
                .findTopByStationIdOrderByRecordedAtDesc(stationId)
                .orElse(null);

        int newTotalQuantity = totalConnectorQuantity(savedStation);
        if (latestStatus != null) {
            int currentTotal = latestStatus.getAvailableCount() + latestStatus.getOccupiedCount() +
                    latestStatus.getReservedCount() + latestStatus.getOutOfServiceCount() +
                    latestStatus.getUnknownCount();

            if (currentTotal != newTotalQuantity) {
                int currentNonAvailable = latestStatus.getOccupiedCount() + latestStatus.getReservedCount() +
                        latestStatus.getOutOfServiceCount() + latestStatus.getUnknownCount();
                int newAvailableCount = Math.max(0, newTotalQuantity - currentNonAvailable);

                StationStatusSnapshot newSnapshot = new StationStatusSnapshot();
                newSnapshot.setStation(savedStation);
                newSnapshot.setSource(MANUAL_SOURCE);
                newSnapshot.setAvailableCount(newAvailableCount);
                newSnapshot.setOccupiedCount(latestStatus.getOccupiedCount());
                newSnapshot.setReservedCount(latestStatus.getReservedCount());
                newSnapshot.setOutOfServiceCount(latestStatus.getOutOfServiceCount());
                newSnapshot.setUnknownCount(latestStatus.getUnknownCount());
                newSnapshot.setRecordedAt(Instant.now());
                latestStatus = snapshotRepository.save(newSnapshot);
            }
        } else {
            StationStatusSnapshot newSnapshot = new StationStatusSnapshot();
            newSnapshot.setStation(savedStation);
            newSnapshot.setSource(MANUAL_SOURCE);
            newSnapshot.setAvailableCount(newTotalQuantity);
            newSnapshot.setOccupiedCount(0);
            newSnapshot.setReservedCount(0);
            newSnapshot.setOutOfServiceCount(0);
            newSnapshot.setUnknownCount(0);
            newSnapshot.setRecordedAt(Instant.now());
            latestStatus = snapshotRepository.save(newSnapshot);
        }

        return StationMapper.toDetailsResponse(savedStation, latestStatus);
    }

    public void deleteStationById(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new NotFoundException("Nie znaleziono stacji o id " + stationId);
        }
        stationRepository.deleteById(stationId);
    }

    @Transactional
    public void fetchStationsFromOCM(Double minLat, Double minLon, Double maxLat, Double maxLon) {
        List<ExternalOCMStation> externalOCMStations = ocmClient.fetchStations(minLat, minLon, maxLat, maxLon);

        List<StationMapper.StationWithSnapshot> mapped = externalOCMStations
                .stream()
                .map(StationMapper::toEntityWithSnapshot)
                .toList();

        // OCM czasem zwraca duplikaty w jednej odpowiedzi – zostawiamy ostatni po kluczu zewnętrznym
        Map<String, StationMapper.StationWithSnapshot> deduplicated = new LinkedHashMap<>();
        for (StationMapper.StationWithSnapshot pair : mapped) {
            String key = pair.station().getExternalSource() + "::" + pair.station().getExternalId();
            deduplicated.put(key, pair);
        }

        List<Station> stationsToSave = new ArrayList<>();
        List<StationStatusSnapshot> snapshotsToSave = new ArrayList<>();
        int insertedCount = 0;
        int updatedCount = 0;

        for (StationMapper.StationWithSnapshot pair : deduplicated.values()) {
            Station incoming = pair.station();
            StationStatusSnapshot incomingSnapshot = pair.snapshot();

            Station stationToSave = stationRepository
                    .findByExternalSourceAndExternalId(incoming.getExternalSource(), incoming.getExternalId())
                    .map(existing -> {
                        applyIncomingStationData(existing, incoming);
                        return existing;
                    })
                    .orElse(incoming);

            if (stationToSave.getId() == null) insertedCount++;
            else updatedCount++;

            stationsToSave.add(stationToSave);
            snapshotsToSave.add(incomingSnapshot);
        }

        List<Station> savedStations = stationRepository.saveAll(stationsToSave);

        for (int i = 0; i < savedStations.size(); i++) {
            snapshotsToSave.get(i).setStation(savedStations.get(i));
        }
        snapshotRepository.saveAll(snapshotsToSave);

        log.info("OCM import finished: total={}, inserted={}, updated={}, snapshots={}",
                savedStations.size(), insertedCount, updatedCount, snapshotsToSave.size());
    }

    private static void applyIncomingStationData(Station target, Station source) {
        target.setName(source.getName());
        target.setLatitude(source.getLatitude());
        target.setLongitude(source.getLongitude());
        target.setAddressLine(source.getAddressLine());
        target.setCity(source.getCity());
        target.setCountry(source.getCountry());
        target.setOperatorName(source.getOperatorName());
        target.setOpeningHours(source.getOpeningHours());
        target.setAccessType(source.getAccessType());
        Instant lockedUntil = target.getAdminActiveLockedUntil();
        if (lockedUntil == null || lockedUntil.isBefore(Instant.now())) {
            target.setActive(source.isActive());
        }
        target.setLastSyncedAt(Instant.now());

        target.getConnectors().clear();
        for (var sourceConnector : source.getConnectors()) {
            sourceConnector.setStation(target);
            target.getConnectors().add(sourceConnector);
        }
    }
}
