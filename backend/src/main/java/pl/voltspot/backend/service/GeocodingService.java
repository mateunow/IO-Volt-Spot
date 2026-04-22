package pl.voltspot.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import pl.voltspot.backend.dto.geocoding.LocationResponse;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.GeocodingException;
import pl.voltspot.backend.exceptions.NotFoundException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class GeocodingService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LocationResponse search(String query) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Parametr query nie może być pusty");
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = URI.create("https://nominatim.openstreetmap.org/search?q=" + encodedQuery + "&format=json&limit=1");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("User-Agent", "VoltSpotApp/1.0 (sample-mail@gmail.com)")
                    .header("Accept-Language", "pl")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new GeocodingException("Błąd podczas komunikacji z serwisem map");
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                throw new NotFoundException("Nie znaleziono lokalizacji dla zapytania: " + query);
            }

            JsonNode first = root.get(0);

            double lat = Double.parseDouble(first.get("lat").asText());
            double lon = Double.parseDouble(first.get("lon").asText());
            String name = first.get("display_name").asText();

            return new LocationResponse(lat, lon, name);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GeocodingException("Błąd podczas komunikacji z serwisem map", ex);
        } catch (IOException ex) {
            throw new GeocodingException("Błąd podczas komunikacji z serwisem map", ex);
        } catch (NumberFormatException ex) {
            throw new GeocodingException("Niepoprawny format danych z geocoding API", ex);
        }
    }
}