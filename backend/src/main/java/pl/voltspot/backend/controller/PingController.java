package pl.voltspot.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class PingController {

    @GetMapping("/ping")
    public String ping() {
        return "Backend Volt Spot działa, połączono z bazą volt_spot_db!";
    }
}
