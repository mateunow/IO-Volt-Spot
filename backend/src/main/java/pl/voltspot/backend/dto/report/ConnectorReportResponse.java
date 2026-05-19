package pl.voltspot.backend.dto.report;

import java.util.List;

public record ConnectorReportResponse(
        List<ConnectorStatusDto> connectorStatuses,
        CommunityOverrideResponse stationOverride
) {}
