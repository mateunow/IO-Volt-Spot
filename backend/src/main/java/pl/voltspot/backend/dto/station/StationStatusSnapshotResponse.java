package pl.voltspot.backend.dto.station;

import java.time.Instant;

public record StationStatusSnapshotResponse(
        Long id,
        String source,
        Integer availableCount,
        Integer occupiedCount,
        Integer reservedCount,
        Integer outOfServiceCount,
        Integer unknownCount,
        Instant recordedAt
) {
}