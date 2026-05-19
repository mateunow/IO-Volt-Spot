package pl.voltspot.backend.dto.report;

import jakarta.validation.constraints.NotNull;
import pl.voltspot.backend.enums.ReportedStatus;

public record StationReportRequest(
        @NotNull ReportedStatus reportedStatus
) {}
