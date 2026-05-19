package pl.voltspot.backend.integration;

import org.junit.jupiter.api.Test;
import pl.voltspot.backend.dto.auth.LoginResponse;
import pl.voltspot.backend.enums.UserRole;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StationFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void listStations_returnsSeedData_withoutFilters() throws Exception {
        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[*].name", hasItem("GreenWay Galeria Kazimierz")))
                .andExpect(jsonPath("$[*].name", hasItem("Orlen EV Kraków Mogilska")));
    }

    @Test
    void listStations_returnsBadRequest_whenBboxIsPartial() throws Exception {
        mockMvc.perform(get("/api/stations")
                        .param("minLat", "49.9")
                        .param("maxLat", "50.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("bbox")));
    }

    @Test
    void listStations_filtersByBbox() throws Exception {
        mockMvc.perform(get("/api/stations")
                        .param("minLat", "0.0")
                        .param("maxLat", "1.0")
                        .param("minLon", "0.0")
                        .param("maxLon", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getStationDetails_returnsFullPayloadWithConnectorsAndStatus() throws Exception {
        mockMvc.perform(get("/api/stations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("GreenWay Galeria Kazimierz"))
                .andExpect(jsonPath("$.city").value("Kraków"))
                .andExpect(jsonPath("$.connectors.length()").value(2))
                .andExpect(jsonPath("$.connectors[*].connectorType", hasItem("CCS2")))
                .andExpect(jsonPath("$.connectors[*].connectorType", hasItem("Type2")))
                .andExpect(jsonPath("$.latestStatus.availableCount").value(2))
                .andExpect(jsonPath("$.latestStatus.occupiedCount").value(1));
    }

    @Test
    void getStationDetails_returns404_forUnknownStation() throws Exception {
        mockMvc.perform(get("/api/stations/9999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLatestStatus_returnsSnapshotForSeedStation() throws Exception {
        mockMvc.perform(get("/api/stations/2/status/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("TOMTOM"))
                .andExpect(jsonPath("$.availableCount").value(1))
                .andExpect(jsonPath("$.occupiedCount").value(2));
    }

    @Test
    void getLatestStatus_returns404_forUnknownStation() throws Exception {
        mockMvc.perform(get("/api/stations/9999999/status/latest"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStation_requiresAuthentication() throws Exception {
        String body = """
                {"name":"Nowa nazwa","latitude":50.0,"longitude":20.0,"active":true}
                """;

        mockMvc.perform(put("/api/stations/1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateStation_requiresAdminRole() throws Exception {
        LoginResponse session = registerUser("regular@example.com", "secret123", "Regular");

        String body = """
                {"name":"Nowa nazwa","latitude":50.0,"longitude":20.0,"active":true}
                """;

        mockMvc.perform(put("/api/stations/1")
                        .header("Authorization", bearer(session))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStation_persistsChangesForAdmin() throws Exception {
        LoginResponse admin = registerUserWithRole(
                "station-admin@example.com", "secret123", "Station Admin", UserRole.ADMIN);

        String body = """
                {
                  "name": "Zmieniona nazwa",
                  "latitude": 51.5,
                  "longitude": 19.5,
                  "addressLine": "ul. Testowa 1",
                  "city": "Łódź",
                  "country": "PL",
                  "operatorName": "TestOp",
                  "openingHours": "Mon-Fri 9-17",
                  "accessType": "private",
                  "active": false
                }
                """;

        mockMvc.perform(put("/api/stations/1")
                        .header("Authorization", bearer(admin))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Zmieniona nazwa"))
                .andExpect(jsonPath("$.city").value("Łódź"))
                .andExpect(jsonPath("$.country").value("PL"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/stations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Zmieniona nazwa"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateStation_returnsBadRequest_whenPayloadInvalid() throws Exception {
        LoginResponse admin = registerUserWithRole(
                "station-admin-2@example.com", "secret123", "Station Admin", UserRole.ADMIN);

        String body = """
                {"name":"","latitude":null,"longitude":null,"active":true}
                """;

        mockMvc.perform(put("/api/stations/1")
                        .header("Authorization", bearer(admin))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteStation_requiresAdmin_andRemovesStation() throws Exception {
        LoginResponse admin = registerUserWithRole(
                "station-admin-3@example.com", "secret123", "Station Admin", UserRole.ADMIN);

        mockMvc.perform(delete("/api/stations/2")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/stations/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteStation_returns404_forUnknownStation() throws Exception {
        LoginResponse admin = registerUserWithRole(
                "station-admin-4@example.com", "secret123", "Station Admin", UserRole.ADMIN);

        mockMvc.perform(delete("/api/stations/9999999")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }
}
