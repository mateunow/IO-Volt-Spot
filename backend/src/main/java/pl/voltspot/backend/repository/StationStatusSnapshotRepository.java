package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.voltspot.backend.entity.StationStatusSnapshot;

import java.util.List;
import java.util.Optional;

public interface StationStatusSnapshotRepository extends JpaRepository<StationStatusSnapshot, Long> {

    Optional<StationStatusSnapshot> findTopByStationIdOrderByRecordedAtDesc(Long stationId);

    List<StationStatusSnapshot> findByStationIdOrderByRecordedAtDesc(Long stationId);

    @Query(value = """
            SELECT DISTINCT ON (station_id) *
            FROM station_status_snapshots
            WHERE station_id IN :stationIds
            ORDER BY station_id, recorded_at DESC
            """, nativeQuery = true)
    List<StationStatusSnapshot> findLatestForStations(@Param("stationIds") List<Long> stationIds);
}