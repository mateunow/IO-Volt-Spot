package pl.voltspot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.voltspot.backend.auth.RequireRole;
import pl.voltspot.backend.dto.station.StationDetailsResponse;
import pl.voltspot.backend.dto.station.StationMarkerResponse;
import pl.voltspot.backend.dto.station.StationStatusSnapshotResponse;
import pl.voltspot.backend.dto.station.UpdateStationRequest;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.service.StationCacheService;
import pl.voltspot.backend.service.StationService;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;
    private final StationCacheService stationCacheService;

    @GetMapping
    public ResponseEntity<List<StationMarkerResponse>> getStations(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        boolean noFilters = minLat == null && maxLat == null && minLon == null && maxLon == null;
        if (!noFilters) {
            return ResponseEntity.ok(stationService.getStations(minLat, maxLat, minLon, maxLon));
        }
        String currentEtag = stationCacheService.getEtag();
        if (currentEtag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok()
                .header("ETag", currentEtag)
                .body(stationCacheService.getCached());
    }

    @GetMapping("/{stationId}")
    public StationDetailsResponse getById(@PathVariable Long stationId) {
        return stationService.getStationById(stationId);
    }

    @GetMapping("/{stationId}/status/latest")
    public StationStatusSnapshotResponse getLatestStatus(@PathVariable Long stationId) {
        return stationService.getLatestStatus(stationId);
    }

    @PutMapping("/{stationId}")
    @RequireRole({UserRole.ADMIN})
    public StationDetailsResponse updateStation(
            @PathVariable Long stationId,
            @Valid @RequestBody UpdateStationRequest request
    ) {
        StationDetailsResponse result = stationService.updateStation(stationId, request);
        stationCacheService.refresh();
        return result;
    }

    @DeleteMapping("/{stationId}")
    @RequireRole({UserRole.ADMIN})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStation(@PathVariable Long stationId) {
        stationService.deleteStationById(stationId);
        stationCacheService.refresh();
    }
}