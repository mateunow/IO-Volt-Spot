package pl.voltspot.backend.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.voltspot.backend.auth.AuthContext;
import pl.voltspot.backend.auth.CurrentUser;
import pl.voltspot.backend.dto.feedback.CreateStationFeedbackRequest;
import pl.voltspot.backend.dto.feedback.StationFeedbackResponse;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationFeedback;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.OperationalStatus;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.ForbiddenException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.exceptions.UnauthorizedException;
import pl.voltspot.backend.repository.StationFeedbackRepository;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationFeedbackServiceTest {

    @Mock
    private StationFeedbackRepository feedbackRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StationFeedbackService service;

    private User author;
    private User otherUser;
    private Station station;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(1L);
        author.setEmail("author@example.com");
        author.setDisplayName("Author");
        author.setRole(UserRole.USER);

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        otherUser.setDisplayName("Other");
        otherUser.setRole(UserRole.USER);

        station = new Station();
        station.setId(50L);
        station.setName("Stacja Y");
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    private void loginAs(User user, UserRole role) {
        AuthContext.set(new CurrentUser(user.getId(), user.getEmail(), user.getDisplayName(), role));
    }

    @Test
    void createFeedback_throwsNotFound_whenStationMissing() {
        loginAs(author, UserRole.USER);
        when(stationRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFeedback(50L,
                new CreateStationFeedbackRequest(OperationalStatus.WORKING, "ok")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("stacji");
    }

    @Test
    void createFeedback_throwsUnauthorized_whenNotLoggedIn() {
        when(stationRepository.findById(50L)).thenReturn(Optional.of(station));

        assertThatThrownBy(() -> service.createFeedback(50L,
                new CreateStationFeedbackRequest(OperationalStatus.WORKING, "ok")))
                .isInstanceOf(UnauthorizedException.class);

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void createFeedback_throwsNotFound_whenUserMissing() {
        loginAs(author, UserRole.USER);
        when(stationRepository.findById(50L)).thenReturn(Optional.of(station));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFeedback(50L,
                new CreateStationFeedbackRequest(OperationalStatus.WORKING, "ok")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("użytkownika");
    }

    @Test
    void createFeedback_savesFeedbackAndReturnsResponse() {
        loginAs(author, UserRole.USER);
        when(stationRepository.findById(50L)).thenReturn(Optional.of(station));
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(feedbackRepository.save(any(StationFeedback.class))).thenAnswer(invocation -> {
            StationFeedback f = invocation.getArgument(0);
            f.setId(700L);
            f.setCreatedAt(Instant.parse("2025-05-10T08:00:00Z"));
            return f;
        });

        StationFeedbackResponse response = service.createFeedback(50L,
                new CreateStationFeedbackRequest(OperationalStatus.BUSY, "kolejka"));

        assertThat(response.id()).isEqualTo(700L);
        assertThat(response.stationId()).isEqualTo(50L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.userDisplayName()).isEqualTo("Author");
        assertThat(response.operationalStatus()).isEqualTo(OperationalStatus.BUSY);
        assertThat(response.comment()).isEqualTo("kolejka");

        ArgumentCaptor<StationFeedback> captor = ArgumentCaptor.forClass(StationFeedback.class);
        verify(feedbackRepository).save(captor.capture());
        StationFeedback saved = captor.getValue();
        assertThat(saved.getStation()).isSameAs(station);
        assertThat(saved.getUser()).isSameAs(author);
        assertThat(saved.getOperationalStatus()).isEqualTo(OperationalStatus.BUSY);
        assertThat(saved.getComment()).isEqualTo("kolejka");
    }

    @Test
    void deleteFeedback_throwsUnauthorized_whenNotLoggedIn() {
        assertThatThrownBy(() -> service.deleteFeedback(50L, 700L))
                .isInstanceOf(UnauthorizedException.class);

        verify(feedbackRepository, never()).delete(any());
    }

    @Test
    void deleteFeedback_throwsNotFound_whenFeedbackMissing() {
        loginAs(author, UserRole.USER);
        when(feedbackRepository.findById(700L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteFeedback(50L, 700L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("opinii");
    }

    @Test
    void deleteFeedback_throwsNotFound_whenFeedbackBelongsToAnotherStation() {
        loginAs(author, UserRole.USER);

        Station differentStation = new Station();
        differentStation.setId(999L);

        StationFeedback feedback = new StationFeedback();
        feedback.setId(700L);
        feedback.setStation(differentStation);
        feedback.setUser(author);

        when(feedbackRepository.findById(700L)).thenReturn(Optional.of(feedback));

        assertThatThrownBy(() -> service.deleteFeedback(50L, 700L))
                .isInstanceOf(NotFoundException.class);

        verify(feedbackRepository, never()).delete(any());
    }

    @Test
    void deleteFeedback_throwsForbidden_whenUserIsNeitherOwnerNorAdmin() {
        loginAs(otherUser, UserRole.USER);

        StationFeedback feedback = new StationFeedback();
        feedback.setId(700L);
        feedback.setStation(station);
        feedback.setUser(author);

        when(feedbackRepository.findById(700L)).thenReturn(Optional.of(feedback));

        assertThatThrownBy(() -> service.deleteFeedback(50L, 700L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("uprawnień");

        verify(feedbackRepository, never()).delete(any());
    }

    @Test
    void deleteFeedback_succeeds_whenAuthorMatches() {
        loginAs(author, UserRole.USER);

        StationFeedback feedback = new StationFeedback();
        feedback.setId(700L);
        feedback.setStation(station);
        feedback.setUser(author);

        when(feedbackRepository.findById(700L)).thenReturn(Optional.of(feedback));

        service.deleteFeedback(50L, 700L);

        verify(feedbackRepository, times(1)).delete(feedback);
    }

    @Test
    void deleteFeedback_succeeds_whenAdminDeletesSomeoneElsesFeedback() {
        loginAs(otherUser, UserRole.ADMIN);

        StationFeedback feedback = new StationFeedback();
        feedback.setId(700L);
        feedback.setStation(station);
        feedback.setUser(author);

        when(feedbackRepository.findById(700L)).thenReturn(Optional.of(feedback));

        service.deleteFeedback(50L, 700L);

        verify(feedbackRepository, times(1)).delete(feedback);
    }

    @Test
    void getFeedbackByStationId_throwsNotFound_whenStationDoesNotExist() {
        when(stationRepository.existsById(50L)).thenReturn(false);

        assertThatThrownBy(() -> service.getFeedbackByStationId(50L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getFeedbackByStationId_mapsRepositoryResults() {
        when(stationRepository.existsById(50L)).thenReturn(true);

        StationFeedback f = new StationFeedback();
        f.setId(1L);
        f.setStation(station);
        f.setUser(author);
        f.setOperationalStatus(OperationalStatus.WORKING);
        f.setComment("ok");
        f.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));

        when(feedbackRepository.findByStationIdOrderByCreatedAtDesc(50L)).thenReturn(List.of(f));

        List<StationFeedbackResponse> result = service.getFeedbackByStationId(50L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().userDisplayName()).isEqualTo("Author");
        assertThat(result.getFirst().operationalStatus()).isEqualTo(OperationalStatus.WORKING);
    }
}
