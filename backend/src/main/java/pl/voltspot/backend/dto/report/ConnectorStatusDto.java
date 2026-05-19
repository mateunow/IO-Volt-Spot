package pl.voltspot.backend.dto.report;

import pl.voltspot.backend.enums.ReportedStatus;

public record ConnectorStatusDto(
        Long connectorId,
        ReportedStatus communityStatus,
        Integer reportedOccupiedCount,
        int reportCount,
        boolean fullyOccupied
) {}
