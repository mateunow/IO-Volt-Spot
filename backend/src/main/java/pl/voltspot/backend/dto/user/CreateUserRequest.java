package pl.voltspot.backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.voltspot.backend.enums.UserRole;

public record CreateUserRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Size(max = 100) String displayName,
        UserRole role
) {
}