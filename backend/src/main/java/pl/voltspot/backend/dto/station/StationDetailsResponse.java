package pl.voltspot.backend.dto.station;

import java.time.Instant;
import java.util.List;

public record StationDetailsResponse(
        Long id,
        String externalSource,
        String externalId,
        String name,
        Double latitude,
        Double longitude,
        String addressLine,
        String city,
        String country,
        String operatorName,
        String openingHours,
        String accessType,
        boolean active,
        Instant lastSyncedAt,
        List<StationConnectorResponse> connectors,
        StationStatusSnapshotResponse latestStatus,
        List<StationOwnerResponse> owners
) {
}