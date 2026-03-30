package pl.voltspot.backend;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/geocoding")
@CrossOrigin(origins = "http://localhost:5173")
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