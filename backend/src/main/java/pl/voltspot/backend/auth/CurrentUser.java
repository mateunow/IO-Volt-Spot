package pl.voltspot.backend.auth;

import pl.voltspot.backend.enums.UserRole;

public record CurrentUser(
        Long id,
        String email,
        String displayName,
        UserRole role
) {
}
