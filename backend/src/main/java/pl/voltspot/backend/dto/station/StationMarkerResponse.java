package pl.voltspot.backend.dto.station;

public record StationMarkerResponse(
        Long id,
        String name,
        Double latitude,
        Double longitude,
        String city,
        String operatorName
) {
}