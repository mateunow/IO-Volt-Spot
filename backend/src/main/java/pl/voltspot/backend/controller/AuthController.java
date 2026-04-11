package pl.voltspot.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pl.voltspot.backend.auth.RequireAuth;
import pl.voltspot.backend.dto.auth.LoginRequest;
import pl.voltspot.backend.dto.auth.LoginResponse;
import pl.voltspot.backend.dto.auth.RegisterRequest;
import pl.voltspot.backend.dto.user.UserResponse;
import pl.voltspot.backend.service.AuthService;
import pl.voltspot.backend.auth.AuthContext;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/logout")
    @RequireAuth
    public void logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logout(authorization.substring(7).trim());
            return;
        }

        authService.logout(request.getHeader("X-Auth-Token"));
    }

    @GetMapping("/me")
    @RequireAuth
    public UserResponse me() {
        return authService.me(AuthContext.get());
    }
}
