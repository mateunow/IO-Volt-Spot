package pl.voltspot.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.voltspot.backend.dto.station.StationOwnerResponse;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationOwner;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.repository.StationOwnerRepository;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationOwnerServiceTest {

    @Mock
    private StationOwnerRepository stationOwnerRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StationOwnerService service;

    private Station station;
    private User ownerUser;
    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        station = new Station();
        station.setId(10L);
        station.setName("Stacja X");

        ownerUser = new User();
        ownerUser.setId(20L);
        ownerUser.setEmail("owner@example.com");
        ownerUser.setDisplayName("Owner");
        ownerUser.setRole(UserRole.OWNER);

        adminUser = new User();
        adminUser.setId(21L);
        adminUser.setEmail("admin@example.com");
        adminUser.setDisplayName("Admin");
        adminUser.setRole(UserRole.ADMIN);

        regularUser = new User();
        regularUser.setId(22L);
        regularUser.setEmail("user@example.com");
        regularUser.setDisplayName("User");
        regularUser.setRole(UserRole.USER);
    }

    @Test
    void assignOwner_throwsNotFound_whenStationIsMissing() {
        when(stationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignOwner(10L, 20L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("stacji");

        verify(stationOwnerRepository, never()).save(any());
    }

    @Test
    void assignOwner_throwsNotFound_whenUserIsMissing() {
        when(stationRepository.findById(10L)).thenReturn(Optional.of(station));
        when(userRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignOwner(10L, 20L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("użytkownika");
    }

    @Test
    void assignOwner_throwsBadRequest_whenUserIsNotOwnerOrAdmin() {
        when(stationRepository.findById(10L)).thenReturn(Optional.of(station));
        when(userRepository.findById(22L)).thenReturn(Optional.of(regularUser));

        assertThatThrownBy(() -> service.assignOwner(10L, 22L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OWNER");

        verify(stationOwnerRepository, never()).save(any());
    }

    @Test
    void assignOwner_doesNothing_whenAssignmentAlreadyExists() {
        when(stationRepository.findById(10L)).thenReturn(Optional.of(station));
        when(userRepository.findById(20L)).thenReturn(Optional.of(ownerUser));
        when(stationOwnerRepository.existsByStationIdAndOwner_Id(10L, 20L)).thenReturn(true);

        service.assignOwner(10L, 20L);

        verify(stationOwnerRepository, never()).save(any());
    }

    @Test
    void assignOwner_persistsAssignment_forOwnerRole() {
        when(stationRepository.findById(10L)).thenReturn(Optional.of(station));
        when(userRepository.findById(20L)).thenReturn(Optional.of(ownerUser));
        when(stationOwnerRepository.existsByStationIdAndOwner_Id(10L, 20L)).thenReturn(false);

        service.assignOwner(10L, 20L);

        ArgumentCaptor<StationOwner> captor = ArgumentCaptor.forClass(StationOwner.class);
        verify(stationOwnerRepository).save(captor.capture());
        StationOwner saved = captor.getValue();
        assertThat(saved.getStation()).isSameAs(station);
        assertThat(saved.getOwner()).isSameAs(ownerUser);
    }

    @Test
    void assignOwner_persistsAssignment_forAdminRole() {
        when(stationRepository.findById(10L)).thenReturn(Optional.of(station));
        when(userRepository.findById(21L)).thenReturn(Optional.of(adminUser));
        when(stationOwnerRepository.existsByStationIdAndOwner_Id(10L, 21L)).thenReturn(false);

        service.assignOwner(10L, 21L);

        verify(stationOwnerRepository).save(any(StationOwner.class));
    }

    @Test
    void getOwnersForStation_throwsNotFound_whenStationDoesNotExist() {
        when(stationRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.getOwnersForStation(10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("stacji");
    }

    @Test
    void getOwnersForStation_mapsResponses() {
        when(stationRepository.existsById(10L)).thenReturn(true);

        StationOwner so = new StationOwner();
        so.setId(500L);
        so.setStation(station);
        so.setOwner(ownerUser);
        so.setAssignedAt(Instant.parse("2025-01-01T00:00:00Z"));
        when(stationOwnerRepository.findByStationIdOrderByAssignedAtAsc(10L)).thenReturn(List.of(so));

        List<StationOwnerResponse> result = service.getOwnersForStation(10L);

        assertThat(result).hasSize(1);
        StationOwnerResponse first = result.getFirst();
        assertThat(first.id()).isEqualTo(500L);
        assertThat(first.ownerId()).isEqualTo(20L);
        assertThat(first.ownerDisplayName()).isEqualTo("Owner");
        assertThat(first.ownerEmail()).isEqualTo("owner@example.com");
    }
}
