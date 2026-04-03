package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.dto.feedback.CreateStationFeedbackRequest;
import pl.voltspot.backend.dto.feedback.StationFeedbackResponse;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationFeedback;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.mapper.StationMapper;
import pl.voltspot.backend.repository.StationFeedbackRepository;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StationFeedbackService {

    private final StationFeedbackRepository feedbackRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;

    public StationFeedbackResponse createFeedback(Long stationId, CreateStationFeedbackRequest request) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono stacji o id " + stationId));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika o id " + request.userId()));

        StationFeedback feedback = new StationFeedback();
        feedback.setStation(station);
        feedback.setUser(user);
        feedback.setOperationalStatus(request.operationalStatus());
        feedback.setComment(request.comment());

        return StationMapper.toFeedbackResponse(feedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public List<StationFeedbackResponse> getFeedbackByStationId(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new NotFoundException("Nie znaleziono stacji o id " + stationId);
        }

        return feedbackRepository.findByStationIdOrderByCreatedAtDesc(stationId)
                .stream()
                .map(StationMapper::toFeedbackResponse)
                .toList();
    }
}