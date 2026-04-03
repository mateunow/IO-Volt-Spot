package pl.voltspot.backend.dto.station;

import java.time.Instant;

public record StationOwnerResponse(
        Long id,
        Long ownerId,
        String ownerDisplayName,
        String ownerEmail,
        Instant assignedAt
) {
}