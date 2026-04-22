package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.voltspot.backend.entity.StationConnector;

import java.util.List;

public interface StationConnectorRepository extends JpaRepository<StationConnector, Long> {
    List<StationConnector> findByStationId(Long stationId);
}