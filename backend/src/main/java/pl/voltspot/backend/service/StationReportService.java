package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.dto.report.CommunityOverrideResponse;
import pl.voltspot.backend.entity.CommunityStatusOverride;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationReport;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.OverrideState;
import pl.voltspot.backend.enums.ReportedStatus;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.repository.CommunityStatusOverrideRepository;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.StationReportRepository;
import pl.voltspot.backend.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StationReportService {

    private static final Logger log = LoggerFactory.getLogger(StationReportService.class);
    private static final int AUTO_CONFIRM_THRESHOLD = 3;
    private static final long CONFIRMED_DURATION_HOURS = 24;
    private static final long PENDING_STALE_HOURS = 48;

    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final CommunityStatusOverrideRepository overrideRepository;
    private final StationReportRepository reportRepository;

    @Transactional
    public CommunityOverrideResponse submitReport(Long stationId, Long reporterId, ReportedStatus reportedStatus) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Stacja nie istnieje: " + stationId));
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new NotFoundException("Użytkownik nie istnieje: " + reporterId));

        CommunityStatusOverride override = overrideRepository
                .findByStationIdAndStateIn(stationId, List.of(OverrideState.PENDING, OverrideState.CONFIRMED))
                .orElseGet(() -> {
                    CommunityStatusOverride fresh = new CommunityStatusOverride();
                    fresh.setStation(station);
                    fresh.setReportedStatus(reportedStatus);
                    return fresh;
                });

        Optional<StationReport> existingReport = override.getId() != null
                ? reportRepository.findByStationIdAndReporterIdAndOverrideId(stationId, reporterId, override.getId())
                : Optional.empty();

        if (existingReport.isPresent()) {
            StationReport existing = existingReport.get();
            if (existing.getReportedStatus() == reportedStatus) {
                throw new BadRequestException("Już zgłosiłeś ten status tej stacji.");
            }
            if (existing.getReportedStatus() == ReportedStatus.WORKING) {
                override.setWorkingCount(override.getWorkingCount() - 1);
            } else {
                override.setNotWorkingCount(override.getNotWorkingCount() - 1);
            }
            if (reportedStatus == ReportedStatus.WORKING) {
                override.setWorkingCount(override.getWorkingCount() + 1);
            } else {
                override.setNotWorkingCount(override.getNotWorkingCount() + 1);
            }
            existing.setReportedStatus(reportedStatus);
            reportRepository.save(existing);
        } else {
            if (reportedStatus == ReportedStatus.WORKING) {
                override.setWorkingCount(override.getWorkingCount() + 1);
            } else {
                override.setNotWorkingCount(override.getNotWorkingCount() + 1);
            }
        }

        ReportedStatus majority = computeMajority(override, reportedStatus);
        override.setReportedStatus(majority);

        int majorityCount = majority == ReportedStatus.WORKING
                ? override.getWorkingCount()
                : override.getNotWorkingCount();

        if (majorityCount >= AUTO_CONFIRM_THRESHOLD && override.getState() == OverrideState.PENDING) {
            override.setState(OverrideState.CONFIRMED);
            override.setExpiresAt(Instant.now().plus(CONFIRMED_DURATION_HOURS, ChronoUnit.HOURS));
            log.info("Override auto-confirmed for station {} (majority: {}, count: {})",
                    stationId, majority, majorityCount);
        }

        CommunityStatusOverride saved = overrideRepository.save(override);

        if (existingReport.isEmpty()) {
            StationReport report = new StationReport();
            report.setStation(station);
            report.setReporter(reporter);
            report.setReportedStatus(reportedStatus);
            report.setOverride(saved);
            reportRepository.save(report);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommunityOverrideResponse> getActiveOverrides() {
        return overrideRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityOverrideResponse> getPendingOverrides() {
        return overrideRepository.findByStateOrderByCreatedAtDesc(OverrideState.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommunityOverrideResponse confirmOverride(Long overrideId, Long adminId) {
        CommunityStatusOverride override = getActiveOverride(overrideId);
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin nie istnieje: " + adminId));

        override.setState(OverrideState.CONFIRMED);
        override.setExpiresAt(Instant.now().plus(CONFIRMED_DURATION_HOURS, ChronoUnit.HOURS));
        override.setConfirmedByAdmin(admin);

        return toResponse(overrideRepository.save(override));
    }

    @Transactional
    public void rejectOverride(Long overrideId) {
        CommunityStatusOverride override = getActiveOverride(overrideId);
        override.setState(OverrideState.REJECTED);
        overrideRepository.save(override);
    }

    @Transactional
    public void expireOverrides() {
        Instant now = Instant.now();

        List<CommunityStatusOverride> expiredConfirmed = overrideRepository.findExpiredConfirmed(now);
        expiredConfirmed.forEach(o -> o.setState(OverrideState.EXPIRED));
        overrideRepository.saveAll(expiredConfirmed);

        Instant staleCutoff = now.minus(PENDING_STALE_HOURS, ChronoUnit.HOURS);
        List<CommunityStatusOverride> stalePending = overrideRepository.findStalePending(staleCutoff);
        stalePending.forEach(o -> o.setState(OverrideState.EXPIRED));
        overrideRepository.saveAll(stalePending);

        if (!expiredConfirmed.isEmpty() || !stalePending.isEmpty()) {
            log.info("Expired overrides: {} confirmed, {} stale pending",
                    expiredConfirmed.size(), stalePending.size());
        }
    }

    private CommunityStatusOverride getActiveOverride(Long overrideId) {
        CommunityStatusOverride override = overrideRepository.findById(overrideId)
                .orElseThrow(() -> new NotFoundException("Override nie istnieje: " + overrideId));
        if (override.getState() == OverrideState.REJECTED || override.getState() == OverrideState.EXPIRED) {
            throw new BadRequestException("Override jest już nieaktywny.");
        }
        return override;
    }

    private static ReportedStatus computeMajority(CommunityStatusOverride override, ReportedStatus latestReported) {
        if (override.getWorkingCount() > override.getNotWorkingCount()) return ReportedStatus.WORKING;
        if (override.getNotWorkingCount() > override.getWorkingCount()) return ReportedStatus.NOT_WORKING;
        return latestReported;
    }

    public CommunityOverrideResponse toResponse(CommunityStatusOverride o) {
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
}
