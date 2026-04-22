package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.auth.CurrentUser;
import pl.voltspot.backend.auth.SessionTokenStore;
import pl.voltspot.backend.dto.auth.LoginRequest;
import pl.voltspot.backend.dto.auth.LoginResponse;
import pl.voltspot.backend.dto.auth.RegisterRequest;
import pl.voltspot.backend.dto.user.UserResponse;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.exceptions.UnauthorizedException;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.mapper.StationMapper;
import pl.voltspot.backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenStore sessionTokenStore;

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Nieprawidłowy email lub hasło"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Konto jest nieaktywne");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Nieprawidłowy email lub hasło");
        }

        return buildLoginResponse(user);
    }

    public LoginResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Użytkownik o takim emailu już istnieje");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setRole(UserRole.USER);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        return buildLoginResponse(savedUser);
    }

    public void logout(String token) {
        sessionTokenStore.revoke(token);
    }

    @Transactional(readOnly = true)
    public UserResponse me(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("Brak aktywnej sesji");
        }

        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika o id " + currentUser.id()));

        return StationMapper.toUserResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        UserResponse userResponse = StationMapper.toUserResponse(user);
        String token = sessionTokenStore.create(new CurrentUser(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole()
        ));

        return new LoginResponse(token, userResponse);
    }
}
