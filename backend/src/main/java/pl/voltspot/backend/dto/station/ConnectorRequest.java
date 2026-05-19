package pl.voltspot.backend.dto.station;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ConnectorRequest(
        @NotBlank(message = "Typ złącza jest wymagany")
        @Size(max = 100, message = "Typ złącza może mieć maksymalnie 100 znaków")
        String connectorType,

        @Size(max = 20, message = "Typ prądu może mieć maksymalnie 20 znaków")
        String currentType,

        @DecimalMin(value = "0.0", inclusive = false, message = "Moc musi być dodatnia")
        BigDecimal powerKw,

        @Min(value = 1, message = "Liczba złączy musi wynosić co najmniej 1")
        @Max(value = 100, message = "Liczba złączy nie może przekraczać 100")
        Integer quantity
) {
}
