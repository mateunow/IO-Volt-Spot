package pl.voltspot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.voltspot.backend.auth.AuthContext;
import pl.voltspot.backend.auth.RequireAuth;
import pl.voltspot.backend.auth.RequireRole;
import pl.voltspot.backend.dto.report.CommunityOverrideResponse;
import pl.voltspot.backend.dto.report.ConnectorReportRequest;
import pl.voltspot.backend.dto.report.ConnectorReportResponse;
import pl.voltspot.backend.dto.report.StationReportRequest;
import pl.voltspot.backend.enums.ReportedStatus;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.service.ConnectorReportService;
import pl.voltspot.backend.service.StationReportService;

import java.util.List;
@RestController
@RequiredArgsConstructor
public class StationReportController {

    private final StationReportService reportService;
    private final ConnectorReportService connectorReportService;

    @PostMapping("/api/stations/{stationId}/report")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireAuth
    public CommunityOverrideResponse submitReport(
            @PathVariable Long stationId,
            @Valid @RequestBody StationReportRequest request
    ) {
        if (request.reportedStatus() == ReportedStatus.OCCUPIED) {
            throw new BadRequestException("Status zajęta należy zgłaszać przez panel złączy");
        }
        Long userId = AuthContext.get().id();
        return reportService.submitReport(stationId, userId, request.reportedStatus());
    }

    @PostMapping("/api/stations/{stationId}/connector-reports")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireAuth
    public ConnectorReportResponse submitConnectorReport(
            @PathVariable Long stationId,
            @Valid @RequestBody ConnectorReportRequest request
    ) {
        Long userId = AuthContext.get().id();
        return connectorReportService.submitReport(
                stationId, userId, request.connectorId(), request.reportedStatus(), request.occupiedCount());
    }

    @GetMapping("/api/community/overrides/active")
    public List<CommunityOverrideResponse> getActiveOverrides() {
        return reportService.getActiveOverrides();
    }

    @GetMapping("/api/admin/overrides")
    @RequireRole({UserRole.ADMIN})
    public List<CommunityOverrideResponse> getPendingOverrides() {
        return reportService.getPendingOverrides();
    }

    @PostMapping("/api/admin/stations/{stationId}/set-status")
    @RequireRole({UserRole.ADMIN})
    public CommunityOverrideResponse setStationStatus(
            @PathVariable Long stationId,
            @Valid @RequestBody StationReportRequest request
    ) {
        Long adminId = AuthContext.get().id();
        return reportService.setStationStatusAsAdmin(stationId, adminId, request.reportedStatus());
    }

    @PostMapping("/api/admin/overrides/{overrideId}/confirm")
    @RequireRole({UserRole.ADMIN})
    public CommunityOverrideResponse confirmOverride(@PathVariable Long overrideId) {
        Long adminId = AuthContext.get().id();
        return reportService.confirmOverride(overrideId, adminId);
    }

    @PostMapping("/api/admin/overrides/{overrideId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireRole({UserRole.ADMIN})
    public void rejectOverride(@PathVariable Long overrideId) {
        reportService.rejectOverride(overrideId);
    }
}
