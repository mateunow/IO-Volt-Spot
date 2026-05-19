package pl.voltspot.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.voltspot.backend.auth.CurrentUser;
import pl.voltspot.backend.auth.SessionTokenStore;
import pl.voltspot.backend.dto.auth.LoginRequest;
import pl.voltspot.backend.dto.auth.LoginResponse;
import pl.voltspot.backend.dto.auth.RegisterRequest;
import pl.voltspot.backend.dto.user.UserResponse;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.exceptions.UnauthorizedException;
import pl.voltspot.backend.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionTokenStore sessionTokenStore;

    @InjectMocks
    private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(42L);
        existingUser.setEmail("user@example.com");
        existingUser.setPasswordHash("hashed-password");
        existingUser.setDisplayName("Existing User");
        existingUser.setRole(UserRole.USER);
        existingUser.setActive(true);
        existingUser.setCreatedAt(Instant.parse("2025-01-01T10:15:30Z"));
        existingUser.setUpdatedAt(Instant.parse("2025-01-01T10:15:30Z"));
    }

    @Test
    void login_returnsTokenAndUserResponse_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("  User@Example.com  ", "plain");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("plain", "hashed-password")).thenReturn(true);
        when(sessionTokenStore.create(any(CurrentUser.class))).thenReturn("token-123");

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("token-123");
        assertThat(response.user().id()).isEqualTo(42L);
        assertThat(response.user().email()).isEqualTo("user@example.com");
        assertThat(response.user().role()).isEqualTo(UserRole.USER);

        ArgumentCaptor<CurrentUser> captor = ArgumentCaptor.forClass(CurrentUser.class);
        verify(sessionTokenStore).create(captor.capture());
        CurrentUser captured = captor.getValue();
        assertThat(captured.id()).isEqualTo(42L);
        assertThat(captured.email()).isEqualTo("user@example.com");
        assertThat(captured.displayName()).isEqualTo("Existing User");
        assertThat(captured.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void login_throwsUnauthorized_whenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "pwd")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Nieprawidłowy email lub hasło");

        verify(sessionTokenStore, never()).create(any());
    }

    @Test
    void login_throwsUnauthorized_whenAccountIsInactive() {
        existingUser.setActive(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "pwd")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("nieaktywne");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(sessionTokenStore, never()).create(any());
    }

    @Test
    void login_throwsUnauthorized_whenPasswordDoesNotMatch() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Nieprawidłowy email lub hasło");

        verify(sessionTokenStore, never()).create(any());
    }

    @Test
    void register_createsUserAndReturnsToken_whenEmailIsAvailable() {
        RegisterRequest request = new RegisterRequest("  New@Example.com  ", "plain-pass", "  New User  ");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-pass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(7L);
            toSave.setCreatedAt(Instant.now());
            return toSave;
        });
        when(sessionTokenStore.create(any(CurrentUser.class))).thenReturn("tok-7");

        LoginResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("tok-7");
        UserResponse user = response.user();
        assertThat(user.id()).isEqualTo(7L);
        assertThat(user.email()).isEqualTo("new@example.com");
        assertThat(user.displayName()).isEqualTo("New User");
        assertThat(user.role()).isEqualTo(UserRole.USER);
        assertThat(user.active()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded");
        assertThat(saved.getDisplayName()).isEqualTo("New User");
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void register_throwsBadRequest_whenEmailAlreadyExists() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("user@example.com", "password", "Some Name")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("już istnieje");

        verify(userRepository, never()).save(any());
        verify(sessionTokenStore, never()).create(any());
    }

    @Test
    void logout_delegatesRevocationToTokenStore() {
        authService.logout("my-token");
        verify(sessionTokenStore, times(1)).revoke("my-token");
    }

    @Test
    void me_throwsUnauthorized_whenCurrentUserIsNull() {
        assertThatThrownBy(() -> authService.me(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Brak aktywnej sesji");
    }

    @Test
    void me_returnsUserResponse_whenUserExists() {
        CurrentUser currentUser = new CurrentUser(42L, "user@example.com", "Existing User", UserRole.USER);
        when(userRepository.findById(42L)).thenReturn(Optional.of(existingUser));

        UserResponse response = authService.me(currentUser);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void me_throwsNotFound_whenUserNoLongerExists() {
        CurrentUser currentUser = new CurrentUser(99L, "x@y.z", "X", UserRole.USER);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(currentUser))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        verify(userRepository).findById(eq(99L));
    }
}
