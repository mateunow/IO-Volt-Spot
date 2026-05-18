package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.client.OCMClient;
import pl.voltspot.backend.dto.external.ExternalOCMStation;
import pl.voltspot.backend.dto.station.StationDetailsResponse;
import pl.voltspot.backend.dto.station.StationMarkerResponse;
import pl.voltspot.backend.dto.station.StationStatusSnapshotResponse;
import pl.voltspot.backend.dto.station.UpdateStationRequest;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationStatusSnapshot;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.mapper.StationMapper;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.StationStatusSnapshotRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Transactional
public class StationService {

    private static final Logger log = LoggerFactory.getLogger(StationService.class);

    private final StationRepository stationRepository;
    private final StationStatusSnapshotRepository snapshotRepository;
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
        station.setLastSyncedAt(Instant.now());

        Station savedStation = stationRepository.save(station);
        StationStatusSnapshot latestStatus = snapshotRepository
                .findTopByStationIdOrderByRecordedAtDesc(stationId)
                .orElse(null);

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
        target.setActive(source.isActive());
        target.setLastSyncedAt(Instant.now());

        target.getConnectors().clear();
        for (var sourceConnector : source.getConnectors()) {
            sourceConnector.setStation(target);
            target.getConnectors().add(sourceConnector);
        }
    }
}
