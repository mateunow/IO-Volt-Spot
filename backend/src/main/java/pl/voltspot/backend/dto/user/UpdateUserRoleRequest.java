package pl.voltspot.backend.dto.user;

import jakarta.validation.constraints.NotNull;
import pl.voltspot.backend.enums.UserRole;

public record UpdateUserRoleRequest(
        @NotNull(message = "Email nie może być pusty")
        String email,
        @NotNull(message = "Rola nie może być pusta")
        UserRole role
) {}
