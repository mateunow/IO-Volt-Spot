package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.voltspot.backend.entity.StationReport;

import java.util.Optional;

public interface StationReportRepository extends JpaRepository<StationReport, Long> {

    Optional<StationReport> findByStationIdAndReporterIdAndOverrideId(Long stationId, Long reporterId, Long overrideId);
}
