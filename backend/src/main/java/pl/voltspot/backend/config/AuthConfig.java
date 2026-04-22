package pl.voltspot.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.voltspot.backend.auth.SessionTokenStore;

@Configuration
public class AuthConfig {

    @Bean
    public SessionTokenStore sessionTokenStore() {
        return new SessionTokenStore();
    }
}
