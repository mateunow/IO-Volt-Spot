package pl.voltspot.backend.integration;

import org.junit.jupiter.api.Test;
import pl.voltspot.backend.dto.auth.LoginResponse;
import pl.voltspot.backend.enums.UserRole;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void register_persistsUserInDatabaseAndReturnsValidToken() throws Exception {
        String body = """
                {"email":"alice@example.com","password":"secret123","displayName":"Alice"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.user.displayName").value("Alice"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.active").value(true))
                .andExpect(jsonPath("$.user.id").value(greaterThan(3)));
    }

    @Test
    void register_rejectsDuplicateEmail() throws Exception {
        registerUser("dup@example.com", "secret123", "Dup");

        String body = """
                {"email":"dup@example.com","password":"secret456","displayName":"Another"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("już istnieje")));
    }

    @Test
    void register_rejectsInvalidPayload() throws Exception {
        String body = """
                {"email":"not-an-email","password":"x","displayName":""}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void login_returnsTokenForValidCredentials() throws Exception {
        registerUser("bob@example.com", "topsecret", "Bob");

        String body = """
                {"email":"bob@example.com","password":"topsecret"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.user.email").value("bob@example.com"));
    }

    @Test
    void login_returns401_forUnknownEmail() throws Exception {
        String body = """
                {"email":"nobody@example.com","password":"whatever"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns401_forWrongPassword() throws Exception {
        registerUser("carol@example.com", "rightone", "Carol");

        String body = """
                {"email":"carol@example.com","password":"wrongone"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returnsCurrentUser_whenAuthenticated() throws Exception {
        LoginResponse session = registerUser("dave@example.com", "topsecret", "Dave");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("dave@example.com"))
                .andExpect(jsonPath("$.displayName").value("Dave"));
    }

    @Test
    void me_returns401_withoutAuthHeader() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returns401_forInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesTokenSoSubsequentMeReturns401() throws Exception {
        LoginResponse session = registerUser("erin@example.com", "topsecret", "Erin");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerUserWithRole_assignsAdminAndExposesItOverMe() throws Exception {
        LoginResponse session = registerUserWithRole(
                "frank-admin@example.com", "topsecret", "Frank Admin", UserRole.ADMIN);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void emailIsLowercasedDuringRegistration() throws Exception {
        String body = """
                {"email":"Greg@Example.COM","password":"secret123","displayName":"Greg"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("greg@example.com"));

        String loginBody = """
                {"email":"GREG@example.com","password":"secret123"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("greg@example.com"));
    }
}
