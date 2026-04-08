package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.client.OCMClient;
import pl.voltspot.backend.dto.external.ExternalOCMStation;
import pl.voltspot.backend.dto.station.StationDetailsResponse;
import pl.voltspot.backend.dto.station.StationMarkerResponse;
import pl.voltspot.backend.dto.station.StationStatusSnapshotResponse;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationStatusSnapshot;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.mapper.StationMapper;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.StationStatusSnapshotRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StationService {

    private final StationRepository stationRepository;
    private final StationStatusSnapshotRepository snapshotRepository;
    private final OCMClient ocmClient;

    @Transactional(readOnly = true)
    public List<StationMarkerResponse> getStations(Double lat, Double lon, Double radiusKm) {
        List<Station> stations;

        boolean noFilters = lat == null && lon == null && radiusKm == null;
        boolean allFilters = lat != null && lon != null && radiusKm != null;

        if (!noFilters && !allFilters) {
            throw new BadRequestException("Podaj albo wszystkie parametry: lat, lon, radiusKm, albo żaden");
        }

        if (noFilters) {
            stations = stationRepository.findByActiveTrueOrderByIdAsc();
        } else {
            if (radiusKm <= 0) {
                throw new BadRequestException("radiusKm musi być większe od 0");
            }

            double latDelta = radiusKm / 111.0;
            double lonDivisor = 111.0 * Math.cos(Math.toRadians(lat));
            if (Math.abs(lonDivisor) < 0.000001) {
                lonDivisor = 111.0;
            }
            double lonDelta = radiusKm / lonDivisor;

            double minLat = lat - latDelta;
            double maxLat = lat + latDelta;
            double minLon = lon - lonDelta;
            double maxLon = lon + lonDelta;

            stations = stationRepository.findByActiveTrueAndLatitudeBetweenAndLongitudeBetweenOrderByIdAsc(
                    minLat, maxLat, minLon, maxLon
            );
        }

        return stations.stream()
                .map(StationMapper::toMarkerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StationDetailsResponse getStationById(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono stacji o id " + stationId));

        StationStatusSnapshot latestStatus = snapshotRepository.findTopByStationIdOrderByRecordedAtDesc(stationId)
                .orElse(null);

        station.getConnectors().size();
        station.getOwners().size();

        return StationMapper.toDetailsResponse(station, latestStatus);
    }

    @Transactional(readOnly = true)
    public StationStatusSnapshotResponse getLatestStatus(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new NotFoundException("Nie znaleziono stacji o id " + stationId);
        }

        StationStatusSnapshot snapshot = snapshotRepository.findTopByStationIdOrderByRecordedAtDesc(stationId)
                .orElseThrow(() -> new NotFoundException("Brak snapshotu statusu dla stacji o id " + stationId));

        return StationMapper.toStatusResponse(snapshot);
    }

    @Transactional
    public void fetchStationsFromOCM(Double minLat, Double minLon, Double maxLat, Double maxLon){
        List<ExternalOCMStation> externalOCMStations = ocmClient.fetchStations(minLat, minLon, maxLat, maxLon);
        List<Station> stations = externalOCMStations
                .stream()
                .map(StationMapper::toEntity)
                .toList();
        stationRepository.saveAll(stations);
        log.info("Successfully saved {} stations to the database.", stations.size());
    }
}