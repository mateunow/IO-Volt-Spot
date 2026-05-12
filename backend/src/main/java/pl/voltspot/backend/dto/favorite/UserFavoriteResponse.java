package pl.voltspot.backend.dto.favorite;

import java.time.Instant;

public record UserFavoriteResponse(
        Long id,
        Long stationId,
        String stationName,
        String city,
        Double latitude,
        Double longitude,
        Instant addedAt
) {
}
