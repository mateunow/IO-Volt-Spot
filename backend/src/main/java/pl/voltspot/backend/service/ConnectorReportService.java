package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.dto.report.CommunityOverrideResponse;
import pl.voltspot.backend.dto.report.ConnectorReportResponse;
import pl.voltspot.backend.dto.report.ConnectorStatusDto;
import pl.voltspot.backend.entity.*;
import pl.voltspot.backend.enums.OverrideState;
import pl.voltspot.backend.enums.ReportedStatus;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.repository.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConnectorReportService {

    private static final Logger log = LoggerFactory.getLogger(ConnectorReportService.class);
    private static final long REPORT_DURATION_HOURS = 1;

    private final StationRepository stationRepository;
    private final StationConnectorRepository connectorRepository;
    private final UserRepository userRepository;
    private final ConnectorReportRepository connectorReportRepository;
    private final CommunityStatusOverrideRepository overrideRepository;

    @Transactional
    public ConnectorReportResponse submitReport(Long stationId, Long reporterId,
                                                 Long connectorId, ReportedStatus status,
                                                 Integer occupiedCount) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Stacja nie istnieje: " + stationId));
        StationConnector connector = connectorRepository.findById(connectorId)
                .orElseThrow(() -> new NotFoundException("Złącze nie istnieje: " + connectorId));
        if (!connector.getStation().getId().equals(stationId)) {
            throw new BadRequestException("Złącze nie należy do tej stacji");
        }
        userRepository.findById(reporterId)
                .orElseThrow(() -> new NotFoundException("Użytkownik nie istnieje: " + reporterId));

        if (status == ReportedStatus.OCCUPIED) {
            int qty = connector.getQuantity() != null ? connector.getQuantity() : 1;
            if (occupiedCount == null || occupiedCount < 0 || occupiedCount > qty) {
                throw new BadRequestException(
                        "occupiedCount musi być między 0 a " + qty + " dla tego złącza");
            }
        }

        ConnectorReport report = connectorReportRepository
                .findByConnectorIdAndReporterId(connectorId, reporterId)
                .orElseGet(() -> {
                    ConnectorReport r = new ConnectorReport();
                    r.setStation(station);
                    r.setConnector(connector);
                    r.setReporter(userRepository.getReferenceById(reporterId));
                    return r;
                });

        report.setReportedStatus(status);
        report.setOccupiedCount(status == ReportedStatus.OCCUPIED ? occupiedCount : null);
        report.setExpiresAt(Instant.now().plus(REPORT_DURATION_HOURS, ChronoUnit.HOURS));
        connectorReportRepository.save(report);

        List<StationConnector> allConnectors = connectorRepository.findByStationId(stationId);
        List<ConnectorReport> activeReports = connectorReportRepository.findActiveByStationId(stationId, Instant.now());
        List<ConnectorStatusDto> statuses = computeStatuses(allConnectors, activeReports);

        boolean allOccupied = !allConnectors.isEmpty()
                && statuses.stream().allMatch(ConnectorStatusDto::fullyOccupied);
        boolean allNotWorking = !allConnectors.isEmpty()
                && statuses.stream().allMatch(s -> s.communityStatus() == ReportedStatus.NOT_WORKING);
        boolean allWorking = !allConnectors.isEmpty()
                && statuses.stream().allMatch(s -> s.communityStatus() == ReportedStatus.WORKING);
        boolean anyWorking = statuses.stream()
                .anyMatch(s -> s.communityStatus() == ReportedStatus.WORKING);
        boolean stationHasNotWorkingOverride = overrideRepository
                .findByStationIdAndStateIn(stationId, List.of(OverrideState.PENDING, OverrideState.CONFIRMED))
                .map(o -> o.getReportedStatus() == ReportedStatus.NOT_WORKING)
                .orElse(false);

        CommunityStatusOverride activeOverride;
        if (allOccupied) {
            activeOverride = createOccupiedOverride(station);
            log.info("All connectors occupied at station {} — OCCUPIED override created", stationId);
        } else if (allNotWorking) {
            activeOverride = createStatusPendingOverride(station, ReportedStatus.NOT_WORKING);
            log.info("All connectors not working at station {} — NOT_WORKING PENDING override created", stationId);
        } else if (allWorking || (stationHasNotWorkingOverride && anyWorking)) {
            activeOverride = createStatusPendingOverride(station, ReportedStatus.WORKING);
            log.info("All connectors working at station {} — WORKING PENDING override created", stationId);
        } else {
            expireAutoOccupiedOverride(stationId);
            activeOverride = overrideRepository
                    .findByStationIdAndStateIn(stationId, List.of(OverrideState.PENDING, OverrideState.CONFIRMED))
                    .orElse(null);
        }

        CommunityOverrideResponse overrideResponse = activeOverride != null ? toOverrideResponse(activeOverride) : null;

        return new ConnectorReportResponse(statuses, overrideResponse);
    }

    @Transactional(readOnly = true)
    public List<ConnectorStatusDto> getConnectorStatuses(Long stationId) {
        List<StationConnector> allConnectors = connectorRepository.findByStationId(stationId);
        return getConnectorStatuses(allConnectors, stationId);
    }

    @Transactional(readOnly = true)
    public List<ConnectorStatusDto> getConnectorStatuses(List<StationConnector> connectors, Long stationId) {
        List<ConnectorReport> activeReports = connectorReportRepository.findActiveByStationId(stationId, Instant.now());
        return computeStatuses(connectors, activeReports);
    }

    @Transactional
    public void expireReports() {
        List<ConnectorReport> expired = connectorReportRepository.findAllExpired(Instant.now());
        if (!expired.isEmpty()) {
            connectorReportRepository.deleteAll(expired);
            log.info("Expired {} connector reports", expired.size());
        }
    }

    private List<ConnectorStatusDto> computeStatuses(List<StationConnector> connectors,
                                                      List<ConnectorReport> activeReports) {
        Map<Long, List<ConnectorReport>> byConnector = activeReports.stream()
                .collect(Collectors.groupingBy(r -> r.getConnector().getId()));

        return connectors.stream()
                .map(c -> computeStatus(c, byConnector.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    private ConnectorStatusDto computeStatus(StationConnector connector, List<ConnectorReport> reports) {
        int qty = connector.getQuantity() != null ? connector.getQuantity() : 1;

        // Najnowszy raport OCCUPIED decyduje o liczbie zajętych (nie MAX)
        int latestOccupied = reports.stream()
                .filter(r -> r.getReportedStatus() == ReportedStatus.OCCUPIED && r.getOccupiedCount() != null)
                .max(Comparator.comparing(ConnectorReport::getExpiresAt))
                .map(ConnectorReport::getOccupiedCount)
                .orElse(0);

        List<ConnectorReport> opReports = reports.stream()
                .filter(r -> r.getReportedStatus() != ReportedStatus.OCCUPIED)
                .toList();

        // Priorytet wyświetlania: NOT_WORKING > OCCUPIED (>0) > WORKING
        ReportedStatus communityStatus = null;
        if (!opReports.isEmpty()) {
            long notWorkingCount = opReports.stream()
                    .filter(r -> r.getReportedStatus() == ReportedStatus.NOT_WORKING)
                    .count();
            long workingCount = opReports.size() - notWorkingCount;
            communityStatus = notWorkingCount >= workingCount
                    ? ReportedStatus.NOT_WORKING
                    : ReportedStatus.WORKING;
        }
        // Zajęte (nawet częściowo) nadpisuje Działa, ale nie Nie działa
        if (communityStatus != ReportedStatus.NOT_WORKING && latestOccupied > 0) {
            communityStatus = ReportedStatus.OCCUPIED;
        }

        boolean fullyOccupied = latestOccupied >= qty;

        return new ConnectorStatusDto(
                connector.getId(),
                communityStatus,
                latestOccupied > 0 ? latestOccupied : null,
                reports.size(),
                fullyOccupied
        );
    }

    private CommunityStatusOverride createStatusPendingOverride(Station station, ReportedStatus targetStatus) {
        Optional<CommunityStatusOverride> existing = overrideRepository.findByStationIdAndStateIn(
                station.getId(), List.of(OverrideState.PENDING, OverrideState.CONFIRMED));

        if (existing.isPresent()) {
            CommunityStatusOverride e = existing.get();
            // Admin-confirmed overrides are untouchable by connector auto-logic
            if (e.getState() == OverrideState.CONFIRMED && e.getConfirmedByAdmin() != null) return e;
            // Already the right pending status — nothing to do
            if (e.getReportedStatus() == targetStatus && e.getState() == OverrideState.PENDING) return e;
            e.setState(OverrideState.EXPIRED);
            overrideRepository.save(e);
        }

        CommunityStatusOverride override = new CommunityStatusOverride();
        override.setStation(station);
        override.setReportedStatus(targetStatus);
        override.setState(OverrideState.PENDING);
        override.setConsecutiveCount(1);
        return overrideRepository.save(override);
    }

    private void expireAutoOccupiedOverride(Long stationId) {
        overrideRepository.findByStationIdAndStateIn(stationId, List.of(OverrideState.CONFIRMED))
                .filter(o -> o.getReportedStatus() == ReportedStatus.OCCUPIED
                        && o.getConfirmedByAdmin() == null)
                .ifPresent(o -> {
                    o.setState(OverrideState.EXPIRED);
                    overrideRepository.save(o);
                    log.info("Auto-OCCUPIED override expired for station {} — connectors no longer all occupied", stationId);
                });
    }

    private CommunityOverrideResponse toOverrideResponse(CommunityStatusOverride o) {
        return new CommunityOverrideResponse(
                o.getId(),
                o.getStation().getId(),
                o.getStation().getName(),
                o.getStation().getCity(),
                o.getReportedStatus(),
                o.getState(),
                o.getWorkingCount(),
                o.getNotWorkingCount(),
                o.getCreatedAt(),
                o.getExpiresAt()
        );
    }

    private CommunityStatusOverride createOccupiedOverride(Station station) {
        Optional<CommunityStatusOverride> existing = overrideRepository.findByStationIdAndStateIn(
                station.getId(), List.of(OverrideState.PENDING, OverrideState.CONFIRMED));
        if (existing.isPresent()) {
            CommunityStatusOverride e = existing.get();
            if (e.getState() == OverrideState.CONFIRMED && e.getConfirmedByAdmin() != null) return e;
            e.setState(OverrideState.EXPIRED);
            overrideRepository.save(e);
        }

        CommunityStatusOverride override = new CommunityStatusOverride();
        override.setStation(station);
        override.setReportedStatus(ReportedStatus.OCCUPIED);
        override.setState(OverrideState.CONFIRMED);
        override.setConfirmedAt(Instant.now());
        override.setConsecutiveCount(1);
        override.setExpiresAt(Instant.now().plus(REPORT_DURATION_HOURS, ChronoUnit.HOURS));
        return overrideRepository.save(override);
    }
}
