package pl.voltspot.backend.dto.user;

import pl.voltspot.backend.enums.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        UserRole role,
        boolean active,
        Instant createdAt
) {
}