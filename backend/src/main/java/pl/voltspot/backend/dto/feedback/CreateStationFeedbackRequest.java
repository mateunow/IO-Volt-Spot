package pl.voltspot.backend.dto.feedback;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.voltspot.backend.enums.OperationalStatus;

public record CreateStationFeedbackRequest(
        @NotNull OperationalStatus operationalStatus,
        @Size(max = 2000) String comment
) {
}