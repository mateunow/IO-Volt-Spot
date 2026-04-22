package pl.voltspot.backend.dto.auth;

import pl.voltspot.backend.dto.user.UserResponse;

public record LoginResponse(
        String token,
        UserResponse user
) {
}
