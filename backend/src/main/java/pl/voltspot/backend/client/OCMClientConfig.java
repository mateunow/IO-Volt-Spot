package pl.voltspot.backend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class OCMClientConfig {
    @Value("${ocm.api.key}")
    private String apiKey;
    @Bean
    public RestClient OCMRestClient(){
        return RestClient.builder()
                .baseUrl("https://api.openchargemap.io/v3")
                .defaultHeader("X-API-Key", apiKey)
                .defaultHeader(HttpHeaders.USER_AGENT, "volt-spot 0.1 (szymonholysz@student.agh.edu.pl)")
                .build();
    }
}
