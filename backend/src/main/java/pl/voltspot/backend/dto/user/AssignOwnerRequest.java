package pl.voltspot.backend.dto.user;

import jakarta.validation.constraints.NotNull;

public record AssignOwnerRequest(
        @NotNull Long ownerUserId
) {
}