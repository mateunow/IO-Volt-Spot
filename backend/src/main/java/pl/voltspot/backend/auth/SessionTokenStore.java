package pl.voltspot.backend.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionTokenStore {

    private static final Duration TOKEN_TTL = Duration.ofHours(12);
    private final Map<String, TokenSession> sessions = new ConcurrentHashMap<>();

    public String create(CurrentUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(TOKEN_TTL);
        sessions.put(token, new TokenSession(user, expiresAt));
        return token;
    }

    public Optional<CurrentUser> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        TokenSession session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }

        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }

        return Optional.of(session.user());
    }

    public void revoke(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public void updateUserRole(Long userId, pl.voltspot.backend.enums.UserRole newRole) {
        sessions.entrySet().forEach(entry -> {
            TokenSession session = entry.getValue();
            if (session.user().id().equals(userId)) {
                CurrentUser updatedUser = new CurrentUser(
                        userId,
                        session.user().email(),
                        session.user().displayName(),
                        newRole
                );
                sessions.put(entry.getKey(), new TokenSession(updatedUser, session.expiresAt()));
            }
        });
    }

    private record TokenSession(CurrentUser user, Instant expiresAt) {
    }
}
