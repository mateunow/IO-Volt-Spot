package pl.voltspot.backend.auth;

import org.junit.jupiter.api.Test;
import pl.voltspot.backend.enums.UserRole;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTokenStoreTest {

    @Test
    void create_andResolve_returnsOriginalUser() {
        SessionTokenStore store = new SessionTokenStore();
        CurrentUser user = new CurrentUser(1L, "a@b.c", "A", UserRole.USER);

        String token = store.create(user);

        assertThat(token).isNotBlank();
        Optional<CurrentUser> resolved = store.resolve(token);
        assertThat(resolved).isPresent();
        assertThat(resolved.get()).isEqualTo(user);
    }

    @Test
    void resolve_returnsEmpty_forUnknownToken() {
        SessionTokenStore store = new SessionTokenStore();
        assertThat(store.resolve("unknown-token")).isEmpty();
    }

    @Test
    void resolve_returnsEmpty_forNullOrBlankToken() {
        SessionTokenStore store = new SessionTokenStore();

        assertThat(store.resolve(null)).isEmpty();
        assertThat(store.resolve("")).isEmpty();
        assertThat(store.resolve("   ")).isEmpty();
    }

    @Test
    void revoke_removesToken() {
        SessionTokenStore store = new SessionTokenStore();
        CurrentUser user = new CurrentUser(1L, "a@b.c", "A", UserRole.USER);
        String token = store.create(user);

        store.revoke(token);

        assertThat(store.resolve(token)).isEmpty();
    }

    @Test
    void revoke_isNoOp_forNullOrUnknownToken() {
        SessionTokenStore store = new SessionTokenStore();

        store.revoke(null);
        store.revoke("nothing-here");
    }

    @Test
    void resolve_returnsEmpty_andEvictsToken_whenExpired() throws Exception {
        SessionTokenStore store = new SessionTokenStore();
        CurrentUser user = new CurrentUser(1L, "a@b.c", "A", UserRole.USER);

        String token = store.create(user);

        Field sessionsField = SessionTokenStore.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> sessions = (Map<String, Object>) sessionsField.get(store);

        Class<?> tokenSessionClass = Class.forName("pl.voltspot.backend.auth.SessionTokenStore$TokenSession");
        var ctor = tokenSessionClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object expiredSession = ctor.newInstance(user, Instant.now().minusSeconds(60));
        sessions.put(token, expiredSession);

        assertThat(store.resolve(token)).isEmpty();
        assertThat(sessions).doesNotContainKey(token);
    }

    @Test
    void create_returnsUniqueTokens() {
        SessionTokenStore store = new SessionTokenStore();
        CurrentUser user = new CurrentUser(1L, "a@b.c", "A", UserRole.USER);

        String t1 = store.create(user);
        String t2 = store.create(user);

        assertThat(t1).isNotEqualTo(t2);
        assertThat(store.resolve(t1)).contains(user);
        assertThat(store.resolve(t2)).contains(user);
    }
}
