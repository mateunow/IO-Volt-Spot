package pl.voltspot.backend;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import pl.voltspot.backend.Exceptions.GeocodingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;

@Service
public class GeocodingService {

    public LocationResponse search(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        StringBuilder sb = getStringBuilder(encodedQuery);

        JSONArray arr = new JSONArray(sb.toString());
        if (!arr.isEmpty()) {
            JSONObject obj = arr.getJSONObject(0);
            double lat = obj.getDouble("lat");
            double lon = obj.getDouble("lon");
            String name = obj.getString("display_name");
            return new LocationResponse(lat, lon, name);
        }
        return null;
    }

    private static @NonNull StringBuilder getStringBuilder(String encodedQuery) {
        try {
            URI uri = new URI("https://nominatim.openstreetmap.org/search?q="
                    + encodedQuery +
                    "&format=json&limit=1");

            URL url = uri.toURL();
            URLConnection conn = url.openConnection();
            conn.setRequestProperty(
                    "User-Agent",
                    "VoltSpotApp/1.0 (sample-mail@gmail.com)"
            );
            conn.setRequestProperty("Accept-Language", "pl");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb;
        }
        catch (IOException | URISyntaxException e) {
            throw new GeocodingException("Błąd podczas komunikacji z serwisem map", e);
        }
    }
}