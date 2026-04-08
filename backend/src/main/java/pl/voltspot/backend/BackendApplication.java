package pl.voltspot.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import pl.voltspot.backend.service.StationService;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner runOnStartup(StationService stationService){
        // Demo zaciągające na start przykładowe ~30 stacji z okolic Krakowa
        return args -> {
            double minLat = 49.95;
            double minLon = 19.85;
            double maxLat = 50.15;
            double maxLon = 20.05;

            stationService.fetchStationsFromOCM(minLat, minLon, maxLat, maxLon);
        };
    }

}
