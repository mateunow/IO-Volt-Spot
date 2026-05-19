package pl.voltspot.backend.dto.station;

import pl.voltspot.backend.enums.ReportedStatus;

import java.math.BigDecimal;

public record StationConnectorResponse(
        Long id,
        String connectorType,
        String currentType,
        BigDecimal powerKw,
        Integer quantity,
        ReportedStatus communityStatus,
        Integer reportedOccupiedCount
) {
}