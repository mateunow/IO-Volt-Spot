package pl.voltspot.backend.dto.station;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateStationRequest(
        @NotBlank(message = "Nazwa stacji jest wymagana")
        @Size(max = 255, message = "Nazwa stacji może mieć maksymalnie 255 znaków")
        String name,

        @NotNull(message = "Szerokość geograficzna jest wymagana")
        @DecimalMin(value = "-90.0", message = "Szerokość geograficzna musi być z zakresu -90..90")
        @DecimalMax(value = "90.0", message = "Szerokość geograficzna musi być z zakresu -90..90")
        Double latitude,

        @NotNull(message = "Długość geograficzna jest wymagana")
        @DecimalMin(value = "-180.0", message = "Długość geograficzna musi być z zakresu -180..180")
        @DecimalMax(value = "180.0", message = "Długość geograficzna musi być z zakresu -180..180")
        Double longitude,

        @Size(max = 255) String addressLine,
        @Size(max = 120) String city,
        @Size(max = 120) String country,
        @Size(max = 255) String operatorName,
        String openingHours,
        @Size(max = 120) String accessType,

        @Valid
        List<ConnectorRequest> connectors
) {
}
