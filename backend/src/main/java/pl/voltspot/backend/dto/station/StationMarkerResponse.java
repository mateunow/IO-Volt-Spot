package pl.voltspot.backend.dto.station;

import java.util.List;

public record StationMarkerResponse(
        Long id,
        String name,
        Double latitude,
        Double longitude,
        String city,
        String operatorName,
        String markerStatus,
        String openingHours,
        List<String> connectorTypes,
        Double maxPowerKw
) {
}