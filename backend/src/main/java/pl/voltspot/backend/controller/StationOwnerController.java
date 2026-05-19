package pl.voltspot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.voltspot.backend.dto.user.AssignOwnerRequest;
import pl.voltspot.backend.dto.station.StationOwnerResponse;
import pl.voltspot.backend.service.StationOwnerService;

import java.util.List;
import pl.voltspot.backend.auth.RequireAuth;
import pl.voltspot.backend.auth.RequireRole;
import pl.voltspot.backend.enums.UserRole;

@RestController
@RequestMapping("/api/stations/{stationId}/owners")
@RequiredArgsConstructor
@RequireAuth
@RequireRole(UserRole.ADMIN)
public class StationOwnerController {

    private final StationOwnerService stationOwnerService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignOwner(
            @PathVariable Long stationId,
            @Valid @RequestBody AssignOwnerRequest request
    ) {
        stationOwnerService.assignOwner(stationId, request.ownerUserId());
    }

    @GetMapping
    public List<StationOwnerResponse> getOwners(@PathVariable Long stationId) {
        return stationOwnerService.getOwnersForStation(stationId);
    }
}