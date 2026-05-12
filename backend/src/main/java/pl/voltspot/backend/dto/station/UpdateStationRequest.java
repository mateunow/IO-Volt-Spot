package pl.voltspot.backend.dto.station;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateStationRequest(
        @NotBlank(message = "Station name is required")
        String name,

        @NotNull(message = "Latitude is required")
        Double latitude,

        @NotNull(message = "Longitude is required")
        Double longitude,

        String addressLine,
        String city,
        String country,
        String operatorName,
        String openingHours,
        String accessType,
        boolean active
) {
}
