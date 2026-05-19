package pl.voltspot.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.voltspot.backend.dto.user.CreateUserRequest;
import pl.voltspot.backend.dto.user.UserResponse;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void create_persistsUserWithDefaultRoleWhenRoleIsNull() {
        CreateUserRequest request = new CreateUserRequest(
                "  Foo@Example.COM  ", "password123", "  John Doe  ", null);

        when(userRepository.findByEmail("  Foo@Example.COM  ")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            u.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
            return u;
        });

        UserResponse response = userService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("foo@example.com");
        assertThat(response.displayName()).isEqualTo("John Doe");
        assertThat(response.role()).isEqualTo(UserRole.USER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("foo@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded");
        assertThat(saved.getDisplayName()).isEqualTo("John Doe");
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void create_usesProvidedRoleWhenSupplied() {
        CreateUserRequest request = new CreateUserRequest(
                "owner@example.com", "password123", "Owner Name", UserRole.OWNER);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(5L);
            u.setCreatedAt(Instant.now());
            return u;
        });

        UserResponse response = userService.create(request);

        assertThat(response.role()).isEqualTo(UserRole.OWNER);
    }

    @Test
    void create_throwsBadRequest_whenEmailExists() {
        CreateUserRequest request = new CreateUserRequest(
                "dup@example.com", "password123", "Dup", UserRole.USER);
        when(userRepository.findByEmail("dup@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("już istnieje");

        verify(userRepository, never()).save(any());
    }

    @Test
    void getById_returnsUser_whenFound() {
        User user = new User();
        user.setId(10L);
        user.setEmail("a@b.c");
        user.setDisplayName("Alice");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        user.setCreatedAt(Instant.now());

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getById(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("a@b.c");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void getById_throwsNotFound_whenUserMissing() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("404");
    }
}
