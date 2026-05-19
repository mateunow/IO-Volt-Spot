package pl.voltspot.backend.integration;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;
import pl.voltspot.backend.client.OCMClient;
import pl.voltspot.backend.dto.external.ExternalOCMStation;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;


@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18");
    }

    @Bean
    @Primary
    public OCMClient ocmClient() {
        OCMClient mock = Mockito.mock(OCMClient.class);
        Mockito.when(mock.fetchStations(any(), any(), any(), any()))
                .thenReturn(List.<ExternalOCMStation>of());
        return mock;
    }
}
