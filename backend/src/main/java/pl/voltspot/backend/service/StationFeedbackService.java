package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.auth.AuthContext;
import pl.voltspot.backend.auth.CurrentUser;
import pl.voltspot.backend.dto.feedback.CreateStationFeedbackRequest;
import pl.voltspot.backend.dto.feedback.StationFeedbackResponse;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationFeedback;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.ForbiddenException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.exceptions.UnauthorizedException;
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

        CurrentUser currentUser = AuthContext.get();
        if (currentUser == null) {
            throw new UnauthorizedException("Wymagane logowanie");
        }

        User user = userRepository.findById(currentUser.id())
            .orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika o id " + currentUser.id()));

        StationFeedback feedback = new StationFeedback();
        feedback.setStation(station);
        feedback.setUser(user);
        feedback.setOperationalStatus(request.operationalStatus());
        feedback.setComment(request.comment());

        return StationMapper.toFeedbackResponse(feedbackRepository.save(feedback));
    }

    public void deleteFeedback(Long stationId, Long feedbackId) {
        CurrentUser currentUser = AuthContext.get();
        if (currentUser == null) {
            throw new UnauthorizedException("Wymagane logowanie");
        }
        if (currentUser.role() != UserRole.ADMIN) {
            throw new ForbiddenException("Brak uprawnień do usunięcia opinii");
        }

        StationFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono opinii o id " + feedbackId));

        if (!feedback.getStation().getId().equals(stationId)) {
            throw new NotFoundException("Nie znaleziono opinii o id " + feedbackId + " dla stacji o id " + stationId);
        }

        feedbackRepository.delete(feedback);
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