package pl.voltspot.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.ForbiddenException;
import pl.voltspot.backend.exceptions.UnauthorizedException;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final SessionTokenStore sessionTokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AuthContext.clear();

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        CurrentUser currentUser = resolveCurrentUser(request);
        if (currentUser != null) {
            AuthContext.set(currentUser);
        }

        boolean requiresAuth = handlerMethod.hasMethodAnnotation(RequireAuth.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAuth.class);
        RequireRole requiredRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requiredRole == null) {
            requiredRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        if ((requiresAuth || requiredRole != null) && currentUser == null) {
            throw new UnauthorizedException("Wymagane logowanie");
        }

        if (requiredRole != null) {
            boolean allowed = Arrays.stream(requiredRole.value())
                    .anyMatch(role -> role == currentUser.role());
            if (!allowed) {
                throw new ForbiddenException("Brak uprawnień do wykonania tej operacji");
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private CurrentUser resolveCurrentUser(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            return sessionTokenStore.resolve(token).orElse(null);
        }

        String token = request.getHeader("X-Auth-Token");
        return sessionTokenStore.resolve(token).orElse(null);
    }
}
