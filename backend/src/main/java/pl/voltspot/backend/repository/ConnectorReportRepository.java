package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.voltspot.backend.entity.ConnectorReport;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConnectorReportRepository extends JpaRepository<ConnectorReport, Long> {

    @Query("SELECT r FROM ConnectorReport r JOIN FETCH r.connector WHERE r.station.id = :stationId AND r.expiresAt > :now")
    List<ConnectorReport> findActiveByStationId(@Param("stationId") Long stationId, @Param("now") Instant now);

    Optional<ConnectorReport> findByConnectorIdAndReporterId(Long connectorId, Long reporterId);

    @Query("SELECT r FROM ConnectorReport r WHERE r.expiresAt <= :now")
    List<ConnectorReport> findAllExpired(@Param("now") Instant now);
}
