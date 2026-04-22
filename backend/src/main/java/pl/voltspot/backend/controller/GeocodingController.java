package pl.voltspot.backend.controller;
import org.springframework.web.bind.annotation.*;
import pl.voltspot.backend.service.GeocodingService;
import pl.voltspot.backend.dto.geocoding.LocationResponse;

@RestController
@RequestMapping("/api/geocoding")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    @GetMapping("/search")
    public LocationResponse search(@RequestParam String query) {
        return geocodingService.search(query);
    }
}