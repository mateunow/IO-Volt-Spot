package pl.voltspot.backend.dto.station;

import java.math.BigDecimal;

public record StationConnectorResponse(
        Long id,
        String connectorType,
        String currentType,
        BigDecimal powerKw,
        Integer quantity
) {
}