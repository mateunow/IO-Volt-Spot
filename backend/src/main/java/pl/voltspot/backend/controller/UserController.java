package pl.voltspot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.voltspot.backend.dto.user.CreateUserRequest;
import pl.voltspot.backend.dto.user.UserResponse;
import pl.voltspot.backend.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PutMapping("/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @pl.voltspot.backend.auth.RequireAuth
    @pl.voltspot.backend.auth.RequireRole(pl.voltspot.backend.enums.UserRole.ADMIN)
    public void updateRole(
            @Valid @RequestBody pl.voltspot.backend.dto.user.UpdateUserRoleRequest request
    ) {
        userService.updateRole(request);
    }
}