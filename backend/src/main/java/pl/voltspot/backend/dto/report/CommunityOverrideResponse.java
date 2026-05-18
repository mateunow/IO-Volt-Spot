package pl.voltspot.backend.dto.report;

import pl.voltspot.backend.enums.OverrideState;
import pl.voltspot.backend.enums.ReportedStatus;

import java.time.Instant;

public record CommunityOverrideResponse(
        Long id,
        Long stationId,
        String stationName,
        String stationCity,
        ReportedStatus reportedStatus,
        OverrideState state,
        int workingCount,
        int notWorkingCount,
        Instant createdAt,
        Instant expiresAt
) {}
