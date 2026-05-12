package pl.voltspot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.voltspot.backend.auth.RequireRole;
import pl.voltspot.backend.dto.station.StationDetailsResponse;
import pl.voltspot.backend.dto.station.StationMarkerResponse;
import pl.voltspot.backend.dto.station.StationStatusSnapshotResponse;
import pl.voltspot.backend.dto.station.UpdateStationRequest;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.service.StationService;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @GetMapping
    public List<StationMarkerResponse> getStations(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon
    ) {
        return stationService.getStations(minLat, maxLat, minLon, maxLon);
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
        return stationService.updateStation(stationId, request);
    }

    @DeleteMapping("/{stationId}")
    @RequireRole({UserRole.ADMIN})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStation(@PathVariable Long stationId) {
        stationService.deleteStationById(stationId);
    }
}