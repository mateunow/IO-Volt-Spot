package pl.voltspot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.voltspot.backend.dto.feedback.CreateStationFeedbackRequest;
import pl.voltspot.backend.dto.feedback.StationFeedbackResponse;
import pl.voltspot.backend.service.StationFeedbackService;

import java.util.List;

@RestController
@RequestMapping("/api/stations/{stationId}/feedback")
@RequiredArgsConstructor
public class StationFeedbackController {

    private final StationFeedbackService stationFeedbackService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StationFeedbackResponse createFeedback(
            @PathVariable Long stationId,
            @Valid @RequestBody CreateStationFeedbackRequest request
    ) {
        return stationFeedbackService.createFeedback(stationId, request);
    }

    @GetMapping
    public List<StationFeedbackResponse> getFeedback(@PathVariable Long stationId) {
        return stationFeedbackService.getFeedbackByStationId(stationId);
    }
}