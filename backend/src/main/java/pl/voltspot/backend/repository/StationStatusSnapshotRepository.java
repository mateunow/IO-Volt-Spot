package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.voltspot.backend.entity.StationStatusSnapshot;

import java.util.List;
import java.util.Optional;

public interface StationStatusSnapshotRepository extends JpaRepository<StationStatusSnapshot, Long> {

    Optional<StationStatusSnapshot> findTopByStationIdOrderByRecordedAtDesc(Long stationId);

    List<StationStatusSnapshot> findByStationIdOrderByRecordedAtDesc(Long stationId);
}