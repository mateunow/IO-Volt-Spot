package pl.voltspot.backend.integration;

import org.junit.jupiter.api.Test;
import pl.voltspot.backend.dto.auth.LoginResponse;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserFavoriteFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void addFavorite_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/favorites/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addFavorite_persistsFavoriteForLoggedInUser() throws Exception {
        LoginResponse session = registerUser("fav-user@example.com", "secret123", "Fav User");

        mockMvc.perform(post("/api/favorites/1")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stationId").value(1))
                .andExpect(jsonPath("$.stationName").value("GreenWay Galeria Kazimierz"))
                .andExpect(jsonPath("$.city").value("Kraków"));

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[*].stationId", hasItem(1)));
    }

    @Test
    void addFavorite_returnsBadRequest_whenAlreadyFavorited() throws Exception {
        LoginResponse session = registerUser("fav-user-2@example.com", "secret123", "Fav User");

        mockMvc.perform(post("/api/favorites/1")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/favorites/1")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("już w ulubionych")));
    }

    @Test
    void addFavorite_returns404_forUnknownStation() throws Exception {
        LoginResponse session = registerUser("fav-user-3@example.com", "secret123", "Fav User");

        mockMvc.perform(post("/api/favorites/9999999")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeFavorite_deletesEntryAndIsIdempotent() throws Exception {
        LoginResponse session = registerUser("fav-user-4@example.com", "secret123", "Fav User");

        mockMvc.perform(post("/api/favorites/3")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/favorites/3")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(delete("/api/favorites/3")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isNoContent());
    }

    @Test
    void getFavorites_returnsEmptyList_forAnonymous() throws Exception {
        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void isFavorited_returnsFalseForAnonymous() throws Exception {
        mockMvc.perform(get("/api/favorites/1/is-favorited"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void isFavorited_reflectsActualStateForLoggedInUser() throws Exception {
        LoginResponse session = registerUser("fav-user-5@example.com", "secret123", "Fav User");

        mockMvc.perform(get("/api/favorites/1/is-favorited")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        mockMvc.perform(post("/api/favorites/1")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/favorites/1/is-favorited")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void favoritesAreIsolatedBetweenUsers() throws Exception {
        LoginResponse alice = registerUser("alice-fav@example.com", "secret123", "Alice");
        LoginResponse bob = registerUser("bob-fav@example.com", "secret123", "Bob");

        mockMvc.perform(post("/api/favorites/1")
                        .header("Authorization", bearer(alice)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
