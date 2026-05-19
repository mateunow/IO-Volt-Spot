package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.dto.user.CreateUserRequest;
import pl.voltspot.backend.dto.user.UpdateUserRoleRequest;
import pl.voltspot.backend.dto.user.UserResponse;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.mapper.StationMapper;
import pl.voltspot.backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final pl.voltspot.backend.auth.SessionTokenStore sessionTokenStore;

    public UserResponse create(CreateUserRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BadRequestException("Użytkownik o takim emailu już istnieje");
        }

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setRole(request.role() != null ? request.role() : UserRole.USER);

        return StationMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika o id " + id));

        return StationMapper.toUserResponse(user);
    }

    public void updateRole(UpdateUserRoleRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika z emailem " + request.email()));

        user.setRole(request.role());
        userRepository.save(user);

        sessionTokenStore.updateUserRole(user.getId(), request.role());
    }
}