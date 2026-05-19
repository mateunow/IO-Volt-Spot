package pl.voltspot.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.dto.auth.LoginResponse;
import pl.voltspot.backend.dto.auth.RegisterRequest;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Tag("integration")
@Import(IntegrationTestConfig.class)
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    protected UserRepository userRepository;


    protected LoginResponse registerUser(String email, String password, String displayName) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, displayName);

        String responseJson = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseJson, LoginResponse.class);
    }

    protected LoginResponse registerUserWithRole(String email, String password, String displayName, UserRole role) throws Exception {
        LoginResponse initial = registerUser(email, password, displayName);

        if (role == UserRole.USER) {
            return initial;
        }

        User user = userRepository.findById(initial.user().id()).orElseThrow();
        user.setRole(role);
        userRepository.saveAndFlush(user);

        return login(email, password);
    }

    protected LoginResponse login(String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);

        String responseJson = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseJson, LoginResponse.class);
    }

    protected String bearer(LoginResponse response) {
        return "Bearer " + response.token();
    }
}
