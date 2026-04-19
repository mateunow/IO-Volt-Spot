package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.voltspot.backend.entity.StationStatusSnapshot;

import java.util.List;
import java.util.Optional;

public interface StationStatusSnapshotRepository extends JpaRepository<StationStatusSnapshot, Long> {

    Optional<StationStatusSnapshot> findTopByStationIdOrderByRecordedAtDesc(Long stationId);

    List<StationStatusSnapshot> findByStationIdOrderByRecordedAtDesc(Long stationId);

    @Query("""
            SELECT s FROM StationStatusSnapshot s
            WHERE s.station.id IN :stationIds
              AND s.recordedAt = (
                  SELECT MAX(s2.recordedAt)
                  FROM StationStatusSnapshot s2
                  WHERE s2.station.id = s.station.id
              )
            """)
    List<StationStatusSnapshot> findLatestForStations(List<Long> stationIds);
}