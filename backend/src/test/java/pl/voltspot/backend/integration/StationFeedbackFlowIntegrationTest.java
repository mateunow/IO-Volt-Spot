package pl.voltspot.backend.integration;

import org.junit.jupiter.api.Test;
import pl.voltspot.backend.dto.auth.LoginResponse;
import pl.voltspot.backend.enums.UserRole;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StationFeedbackFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getFeedback_returnsSeedDataForStation() throws Exception {
        mockMvc.perform(get("/api/stations/1/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].stationId").value(1));
    }

    @Test
    void getFeedback_returns404_forUnknownStation() throws Exception {
        mockMvc.perform(get("/api/stations/9999999/feedback"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createFeedback_requiresAuthentication() throws Exception {
        String body = """
                {"operationalStatus":"WORKING","comment":"świetnie"}
                """;

        mockMvc.perform(post("/api/stations/1/feedback")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createFeedback_persistsAndIsReturnedInList() throws Exception {
        LoginResponse session = registerUser("feedback-user@example.com", "secret123", "FB User");

        String body = """
                {"operationalStatus":"BUSY","comment":"kolejka"}
                """;

        mockMvc.perform(post("/api/stations/1/feedback")
                        .header("Authorization", bearer(session))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.notNullValue()))
                .andExpect(jsonPath("$.stationId").value(1))
                .andExpect(jsonPath("$.userDisplayName").value("FB User"))
                .andExpect(jsonPath("$.operationalStatus").value("BUSY"))
                .andExpect(jsonPath("$.comment").value("kolejka"));
    }

    @Test
    void createFeedback_returnsBadRequest_whenOperationalStatusMissing() throws Exception {
        LoginResponse session = registerUser("feedback-user-2@example.com", "secret123", "FB User");

        String body = """
                {"comment":"brak statusu"}
                """;

        mockMvc.perform(post("/api/stations/1/feedback")
                        .header("Authorization", bearer(session))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFeedback_returns404_forUnknownStation() throws Exception {
        LoginResponse session = registerUser("feedback-user-3@example.com", "secret123", "FB User");

        String body = """
                {"operationalStatus":"WORKING","comment":"x"}
                """;

        mockMvc.perform(post("/api/stations/9999999/feedback")
                        .header("Authorization", bearer(session))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFeedback_allowsAuthorToDeleteOwnEntry() throws Exception {
        LoginResponse session = registerUser("feedback-author@example.com", "secret123", "Author");

        String body = """
                {"operationalStatus":"WORKING","comment":"moja opinia"}
                """;

        String response = mockMvc.perform(post("/api/stations/1/feedback")
                        .header("Authorization", bearer(session))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long feedbackId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/stations/1/feedback/" + feedbackId)
                        .header("Authorization", bearer(session)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteFeedback_forbidsRegularUserDeletingSomeoneElsesEntry() throws Exception {
        LoginResponse author = registerUser("feedback-author-2@example.com", "secret123", "Author");
        LoginResponse intruder = registerUser("feedback-intruder@example.com", "secret123", "Intruder");

        String body = """
                {"operationalStatus":"WORKING","comment":"moja opinia"}
                """;

        String response = mockMvc.perform(post("/api/stations/1/feedback")
                        .header("Authorization", bearer(author))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long feedbackId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/stations/1/feedback/" + feedbackId)
                        .header("Authorization", bearer(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteFeedback_allowsAdminToDeleteAnyEntry() throws Exception {
        LoginResponse author = registerUser("feedback-author-3@example.com", "secret123", "Author");
        LoginResponse admin = registerUserWithRole(
                "feedback-admin@example.com", "secret123", "Admin", UserRole.ADMIN);

        String body = """
                {"operationalStatus":"NOT_WORKING","comment":"nie działa"}
                """;

        String response = mockMvc.perform(post("/api/stations/1/feedback")
                        .header("Authorization", bearer(author))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long feedbackId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/stations/1/feedback/" + feedbackId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteFeedback_returnsNotFound_whenFeedbackBelongsToDifferentStation() throws Exception {
        LoginResponse session = registerUser("feedback-mismatch@example.com", "secret123", "FB User");

        String body = """
                {"operationalStatus":"WORKING","comment":"x"}
                """;

        String response = mockMvc.perform(post("/api/stations/1/feedback")
                        .header("Authorization", bearer(session))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long feedbackId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/stations/2/feedback/" + feedbackId)
                        .header("Authorization", bearer(session)))
                .andExpect(status().isNotFound());
    }
}
