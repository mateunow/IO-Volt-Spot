package pl.voltspot.backend.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.voltspot.backend.dto.external.ExternalOCMStation;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class OCMClient{

    private final RestClient restClient;
    public List<ExternalOCMStation> fetchStations(Double minLat, Double minLon, Double maxLat, Double maxLon) {
        String bbox = String.format(Locale.US, "(%f,%f),(%f,%f)", minLat, minLon, maxLat, maxLon);


        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/poi")
                        .queryParam("boundingbox", bbox)
                        .queryParam("camelCase", "true")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new RuntimeException("External API error: " + response.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new RuntimeException("OCM Server is currently unavailable");
                })
                .body(new ParameterizedTypeReference<List<ExternalOCMStation>>() {});
    }
}