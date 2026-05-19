package pl.voltspot.backend.integration;

import org.junit.jupiter.api.Test;
import pl.voltspot.backend.dto.auth.LoginResponse;
import pl.voltspot.backend.enums.UserRole;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void updateStation_forbidsRegularUser() throws Exception {
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
    void updateStation_forbidsOwnerOfDifferentStation() throws Exception {
        LoginResponse owner = registerUserWithRole(
                "edit-foreign-owner@example.com", "secret123", "Foreign Owner", UserRole.OWNER);

        String body = """
                {"name":"Hack","latitude":50.0,"longitude":20.0,"active":true}
                """;

        // Stacja 1 ma seedowanego ownera (id=1), nie tego użytkownika
        mockMvc.perform(put("/api/stations/1")
                        .header("Authorization", bearer(owner))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStation_allowsOwnerToEditOwnStation() throws Exception {
        LoginResponse owner = registerUserWithRole(
                "edit-own-owner@example.com", "secret123", "Edit Own Owner", UserRole.OWNER);

        String createBody = """
                {
                  "name": "Stacja właściciela",
                  "latitude": 52.2,
                  "longitude": 21.1,
                  "connectors": [
                    {"connectorType": "CCS2", "currentType": "DC", "powerKw": 50, "quantity": 1}
                  ]
                }
                """;

        String createdJson = mockMvc.perform(post("/api/stations")
                        .header("Authorization", bearer(owner))
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number createdId = (Number) objectMapper.readValue(createdJson, java.util.Map.class).get("id");

        String updateBody = """
                {
                  "name": "Zmieniona przez OWNERa",
                  "latitude": 52.3,
                  "longitude": 21.2,
                  "active": true,
                  "connectors": [
                    {"connectorType": "Type2", "currentType": "AC", "powerKw": 22, "quantity": 2}
                  ]
                }
                """;

        mockMvc.perform(put("/api/stations/" + createdId)
                        .header("Authorization", bearer(owner))
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Zmieniona przez OWNERa"))
                .andExpect(jsonPath("$.connectors.length()").value(1))
                .andExpect(jsonPath("$.connectors[0].connectorType").value("Type2"))
                .andExpect(jsonPath("$.connectors[0].quantity").value(2));
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
    void createStation_requiresAuthentication() throws Exception {
        String body = """
                {"name":"Nowa stacja","latitude":52.0,"longitude":21.0}
                """;

        mockMvc.perform(post("/api/stations")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createStation_forbidsRegularUser() throws Exception {
        LoginResponse session = registerUser("create-regular@example.com", "secret123", "Regular");

        String body = """
                {"name":"Nowa stacja","latitude":52.0,"longitude":21.0}
                """;

        mockMvc.perform(post("/api/stations")
                        .header("Authorization", bearer(session))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createStation_persistsStation_andAssignsOwner_andAppearsInListing() throws Exception {
        LoginResponse owner = registerUserWithRole(
                "create-owner@example.com", "secret123", "Owner User", UserRole.OWNER);

        String body = """
                {
                  "name": "Moja stacja",
                  "latitude": 52.123,
                  "longitude": 21.456,
                  "addressLine": "ul. Nowa 1",
                  "city": "Warszawa",
                  "country": "PL",
                  "operatorName": "Mój operator",
                  "openingHours": "24/7",
                  "accessType": "public"
                }
                """;

        String responseJson = mockMvc.perform(post("/api/stations")
                        .header("Authorization", bearer(owner))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Moja stacja"))
                .andExpect(jsonPath("$.latitude").value(52.123))
                .andExpect(jsonPath("$.longitude").value(21.456))
                .andExpect(jsonPath("$.externalSource").value("MANUAL"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.owners.length()").value(1))
                .andExpect(jsonPath("$.owners[0].ownerEmail").value("create-owner@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number createdId = (Number) objectMapper.readValue(responseJson, java.util.Map.class).get("id");

        mockMvc.perform(get("/api/stations/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Moja stacja"));

        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Moja stacja")));
    }

    @Test
    void createStation_returnsBadRequest_whenPayloadInvalid() throws Exception {
        LoginResponse owner = registerUserWithRole(
                "create-owner-2@example.com", "secret123", "Owner User", UserRole.OWNER);

        String body = """
                {"name":"","latitude":null,"longitude":null}
                """;

        mockMvc.perform(post("/api/stations")
                        .header("Authorization", bearer(owner))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteStation_returns404_forUnknownStation() throws Exception {
        LoginResponse admin = registerUserWithRole(
                "station-admin-4@example.com", "secret123", "Station Admin", UserRole.ADMIN);

        mockMvc.perform(delete("/api/stations/9999999")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteStation_forbidsOwnerOfDifferentStation() throws Exception {
        LoginResponse owner = registerUserWithRole(
                "delete-foreign-owner@example.com", "secret123", "Foreign Owner", UserRole.OWNER);

        // Stacja 1 ma seedowanego ownera (id=1), nie tego użytkownika
        mockMvc.perform(delete("/api/stations/1")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteStation_allowsOwnerToDeleteOwnStation() throws Exception {
        LoginResponse owner = registerUserWithRole(
                "delete-own-owner@example.com", "secret123", "Delete Own Owner", UserRole.OWNER);

        String createBody = """
                {"name":"Stacja do usunięcia","latitude":52.5,"longitude":21.5}
                """;

        String createdJson = mockMvc.perform(post("/api/stations")
                        .header("Authorization", bearer(owner))
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number createdId = (Number) objectMapper.readValue(createdJson, java.util.Map.class).get("id");

        mockMvc.perform(delete("/api/stations/" + createdId)
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/stations/" + createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createStation_withConnectors_persistsThemAndReturnsInResponse() throws Exception {
        LoginResponse owner = registerUserWithRole(
                "create-owner-conn@example.com", "secret123", "Owner Conn", UserRole.OWNER);

        String body = """
                {
                  "name": "Stacja z połączeniami",
                  "latitude": 52.4,
                  "longitude": 21.4,
                  "connectors": [
                    {"connectorType": "CCS2", "currentType": "DC", "powerKw": 150, "quantity": 2},
                    {"connectorType": "Type2", "currentType": "AC", "powerKw": 22.5, "quantity": 1}
                  ]
                }
                """;

        mockMvc.perform(post("/api/stations")
                        .header("Authorization", bearer(owner))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.connectors.length()").value(2))
                .andExpect(jsonPath("$.connectors[*].connectorType", hasItem("CCS2")))
                .andExpect(jsonPath("$.connectors[*].connectorType", hasItem("Type2")))
                .andExpect(jsonPath("$.latestStatus.availableCount").value(3));
    }
}
