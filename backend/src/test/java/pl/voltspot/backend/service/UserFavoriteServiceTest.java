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
import pl.voltspot.backend.dto.favorite.UserFavoriteResponse;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.entity.UserFavorite;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.exceptions.UnauthorizedException;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.UserFavoriteRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFavoriteServiceTest {

    @Mock
    private UserFavoriteRepository userFavoriteRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserFavoriteService service;

    private User currentUserEntity;
    private Station station;

    @BeforeEach
    void setUp() {
        currentUserEntity = new User();
        currentUserEntity.setId(1L);
        currentUserEntity.setEmail("user@example.com");
        currentUserEntity.setDisplayName("Jan");
        currentUserEntity.setRole(UserRole.USER);

        station = new Station();
        station.setId(100L);
        station.setName("Stacja A");
        station.setLatitude(50.0);
        station.setLongitude(20.0);
        station.setCity("Kraków");
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    private void setLoggedInUser() {
        AuthContext.set(new CurrentUser(1L, "user@example.com", "Jan", UserRole.USER));
    }

    @Test
    void addFavorite_throwsUnauthorized_whenNoAuthContext() {
        assertThatThrownBy(() -> service.addFavorite(100L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Musisz być zalogowany");

        verifyNoInteractions(userFavoriteRepository, stationRepository, userRepository);
    }

    @Test
    void addFavorite_throwsNotFound_whenUserMissing() {
        setLoggedInUser();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addFavorite(100L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Użytkownik nie znaleziony");

        verify(userFavoriteRepository, never()).save(any());
    }

    @Test
    void addFavorite_throwsNotFound_whenStationMissing() {
        setLoggedInUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUserEntity));
        when(stationRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addFavorite(100L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Stacja nie znaleziona");
    }

    @Test
    void addFavorite_throwsBadRequest_whenStationAlreadyFavorited() {
        setLoggedInUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUserEntity));
        when(stationRepository.findById(100L)).thenReturn(Optional.of(station));
        when(userFavoriteRepository.existsByUserIdAndStationId(1L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> service.addFavorite(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("już w ulubionych");

        verify(userFavoriteRepository, never()).save(any());
    }

    @Test
    void addFavorite_persistsFavoriteAndReturnsResponse() {
        setLoggedInUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUserEntity));
        when(stationRepository.findById(100L)).thenReturn(Optional.of(station));
        when(userFavoriteRepository.existsByUserIdAndStationId(1L, 100L)).thenReturn(false);
        when(userFavoriteRepository.save(any(UserFavorite.class))).thenAnswer(invocation -> {
            UserFavorite f = invocation.getArgument(0);
            f.setId(999L);
            f.setAddedAt(Instant.parse("2025-05-01T12:00:00Z"));
            return f;
        });

        UserFavoriteResponse response = service.addFavorite(100L);

        assertThat(response.id()).isEqualTo(999L);
        assertThat(response.stationId()).isEqualTo(100L);
        assertThat(response.stationName()).isEqualTo("Stacja A");
        assertThat(response.city()).isEqualTo("Kraków");
        assertThat(response.latitude()).isEqualTo(50.0);
        assertThat(response.longitude()).isEqualTo(20.0);

        ArgumentCaptor<UserFavorite> captor = ArgumentCaptor.forClass(UserFavorite.class);
        verify(userFavoriteRepository).save(captor.capture());
        UserFavorite saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(currentUserEntity);
        assertThat(saved.getStation()).isSameAs(station);
    }

    @Test
    void removeFavorite_throwsUnauthorized_whenNoAuthContext() {
        assertThatThrownBy(() -> service.removeFavorite(100L))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(userFavoriteRepository);
    }

    @Test
    void removeFavorite_delegatesToRepository() {
        setLoggedInUser();

        service.removeFavorite(100L);

        verify(userFavoriteRepository, times(1)).deleteByUserIdAndStationId(1L, 100L);
    }

    @Test
    void getUserFavorites_returnsEmptyList_whenNotLoggedIn() {
        List<UserFavoriteResponse> result = service.getUserFavorites();

        assertThat(result).isEmpty();
        verifyNoInteractions(userFavoriteRepository);
    }

    @Test
    void getUserFavorites_mapsFavoritesFromRepository() {
        setLoggedInUser();
        UserFavorite f1 = new UserFavorite();
        f1.setId(1L);
        f1.setStation(station);
        f1.setAddedAt(Instant.parse("2025-01-01T00:00:00Z"));

        when(userFavoriteRepository.findByUserId(1L)).thenReturn(List.of(f1));

        List<UserFavoriteResponse> result = service.getUserFavorites();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().stationId()).isEqualTo(100L);
        assertThat(result.getFirst().stationName()).isEqualTo("Stacja A");
    }

    @Test
    void isFavorited_returnsFalseWhenNotLoggedIn() {
        assertThat(service.isFavorited(100L)).isFalse();
        verifyNoInteractions(userFavoriteRepository);
    }

    @Test
    void isFavorited_delegatesToRepository_whenLoggedIn() {
        setLoggedInUser();
        when(userFavoriteRepository.existsByUserIdAndStationId(1L, 100L)).thenReturn(true);

        assertThat(service.isFavorited(100L)).isTrue();
    }
}
