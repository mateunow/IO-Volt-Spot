package pl.voltspot.backend.dto.feedback;

import pl.voltspot.backend.enums.OperationalStatus;

import java.time.Instant;

public record StationFeedbackResponse(
        Long id,
        Long stationId,
        Long userId,
        String userDisplayName,
        OperationalStatus operationalStatus,
        String comment,
        Instant createdAt
) {
}