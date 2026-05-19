package pl.voltspot.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.voltspot.backend.client.OCMClient;
import pl.voltspot.backend.dto.station.ConnectorRequest;
import pl.voltspot.backend.dto.station.CreateStationRequest;
import pl.voltspot.backend.dto.station.StationDetailsResponse;
import pl.voltspot.backend.dto.station.StationMarkerResponse;
import pl.voltspot.backend.dto.station.StationStatusSnapshotResponse;
import pl.voltspot.backend.dto.station.UpdateStationRequest;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationConnector;
import pl.voltspot.backend.entity.StationOwner;
import pl.voltspot.backend.entity.StationStatusSnapshot;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.ForbiddenException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.repository.StationOwnerRepository;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.StationStatusSnapshotRepository;
import pl.voltspot.backend.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private StationStatusSnapshotRepository snapshotRepository;

    @Mock
    private StationOwnerRepository stationOwnerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OCMClient ocmClient;

    @InjectMocks
    private StationService service;

    private Station station;
    private StationStatusSnapshot snapshot;

    @BeforeEach
    void setUp() {
        station = new Station();
        station.setId(1L);
        station.setExternalSource("OCM");
        station.setExternalId("ext-1");
        station.setName("Stacja Testowa");
        station.setLatitude(50.0);
        station.setLongitude(20.0);
        station.setCity("Kraków");
        station.setOperatorName("Op");
        station.setOpeningHours("24/7");
        station.setAccessType("public");
        station.setActive(true);
        station.setLastSyncedAt(Instant.parse("2025-05-01T00:00:00Z"));

        StationConnector connector = new StationConnector();
        connector.setId(1L);
        connector.setStation(station);
        connector.setConnectorType("CCS");
        connector.setCurrentType("DC");
        connector.setPowerKw(new BigDecimal("50.00"));
        connector.setQuantity(2);
        station.getConnectors().add(connector);

        snapshot = new StationStatusSnapshot();
        snapshot.setId(10L);
        snapshot.setStation(station);
        snapshot.setSource("OCM");
        snapshot.setAvailableCount(2);
        snapshot.setOccupiedCount(0);
        snapshot.setReservedCount(0);
        snapshot.setOutOfServiceCount(0);
        snapshot.setUnknownCount(0);
        snapshot.setRecordedAt(Instant.parse("2025-05-10T00:00:00Z"));
    }

    @Test
    void getStations_returnsAll_whenNoFiltersProvided() {
        when(stationRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(station));
        when(snapshotRepository.findLatestForStations(anyList())).thenReturn(List.of(snapshot));

        List<StationMarkerResponse> result = service.getStations(null, null, null, null);

        assertThat(result).hasSize(1);
        StationMarkerResponse first = result.getFirst();
        assertThat(first.id()).isEqualTo(1L);
        assertThat(first.name()).isEqualTo("Stacja Testowa");
        assertThat(first.markerStatus()).isEqualTo("WORKING");
        assertThat(first.connectorTypes()).containsExactly("CCS");
        assertThat(first.maxPowerKw()).isEqualTo(50.0);

        verify(ocmClient, never()).fetchStations(any(), any(), any(), any());
    }

    @Test
    void getStations_throwsBadRequest_whenOnlySomeFiltersProvided() {
        assertThatThrownBy(() -> service.getStations(49.9, null, 19.9, 20.1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("bbox");

        verifyNoInteractions(stationRepository, snapshotRepository, ocmClient);
    }

    @Test
    void getStations_returnsEmptyList_whenNoStationsFound() {
        when(stationRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of());

        List<StationMarkerResponse> result = service.getStations(null, null, null, null);

        assertThat(result).isEmpty();
        verify(snapshotRepository, never()).findLatestForStations(anyList());
    }

    @Test
    void getStations_withBbox_returnsDbResults_withoutCallingOcm() {
        when(stationRepository.findByActiveTrueAndLatitudeBetweenAndLongitudeBetweenOrderByIdAsc(
                49.9, 50.1, 19.9, 20.1)).thenReturn(List.of(station));
        when(snapshotRepository.findLatestForStations(anyList())).thenReturn(List.of(snapshot));

        List<StationMarkerResponse> result = service.getStations(49.9, 50.1, 19.9, 20.1);

        assertThat(result).hasSize(1);
        verify(stationRepository).findByActiveTrueAndLatitudeBetweenAndLongitudeBetweenOrderByIdAsc(
                49.9, 50.1, 19.9, 20.1);
        verify(ocmClient, never()).fetchStations(any(), any(), any(), any());
    }

    @Test
    void getStationById_returnsDetails_whenFound() {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(snapshotRepository.findTopByStationIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(snapshot));

        StationDetailsResponse response = service.getStationById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Stacja Testowa");
        assertThat(response.latestStatus()).isNotNull();
        assertThat(response.latestStatus().availableCount()).isEqualTo(2);
    }

    @Test
    void getStationById_returnsDetailsWithoutStatus_whenNoSnapshot() {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(snapshotRepository.findTopByStationIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.empty());

        StationDetailsResponse response = service.getStationById(1L);

        assertThat(response.latestStatus()).isNull();
    }

    @Test
    void getStationById_throwsNotFound_whenStationMissing() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStationById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getLatestStatus_throwsNotFound_whenStationDoesNotExist() {
        when(stationRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.getLatestStatus(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("stacji");
    }

    @Test
    void getLatestStatus_throwsNotFound_whenNoSnapshot() {
        when(stationRepository.existsById(1L)).thenReturn(true);
        when(snapshotRepository.findTopByStationIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatestStatus(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("snapshotu");
    }

    @Test
    void getLatestStatus_returnsSnapshot_whenAvailable() {
        when(stationRepository.existsById(1L)).thenReturn(true);
        when(snapshotRepository.findTopByStationIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(snapshot));

        StationStatusSnapshotResponse response = service.getLatestStatus(1L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.availableCount()).isEqualTo(2);
    }

    @Test
    void createStation_throwsNotFound_whenCreatorMissing() {
        CreateStationRequest request = new CreateStationRequest(
                "Nowa", 52.0, 21.0, null, null, null, null, null, null, null);
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createStation(request, 42L))
                .isInstanceOf(NotFoundException.class);

        verify(stationRepository, never()).save(any());
        verify(stationOwnerRepository, never()).save(any());
    }

    @Test
    void createStation_throwsForbidden_whenCreatorHasUserRole() {
        User regular = new User();
        regular.setId(5L);
        regular.setRole(UserRole.USER);
        CreateStationRequest request = new CreateStationRequest(
                "Nowa", 52.0, 21.0, null, null, null, null, null, null, null);
        when(userRepository.findById(5L)).thenReturn(Optional.of(regular));

        assertThatThrownBy(() -> service.createStation(request, 5L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("OWNER");

        verify(stationRepository, never()).save(any());
        verify(stationOwnerRepository, never()).save(any());
    }

    @Test
    void createStation_persistsStationSnapshotAndAssignsOwner() {
        User owner = new User();
        owner.setId(7L);
        owner.setRole(UserRole.OWNER);

        CreateStationRequest request = new CreateStationRequest(
                "  Stacja Nowa  ",
                52.1,
                21.1,
                "  ul. Testowa 1 ",
                " Warszawa ",
                "PL",
                "Operator",
                "24/7",
                "public",
                null
        );

        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> simulatePersist(inv.getArgument(0), 123L));
        when(snapshotRepository.save(any(StationStatusSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stationOwnerRepository.save(any(StationOwner.class))).thenAnswer(inv -> {
            StationOwner so = inv.getArgument(0);
            if (so.getAssignedAt() == null) so.setAssignedAt(Instant.now());
            return so;
        });

        StationDetailsResponse response = service.createStation(request, 7L);

        ArgumentCaptor<Station> stationCaptor = ArgumentCaptor.forClass(Station.class);
        verify(stationRepository).save(stationCaptor.capture());
        Station savedStation = stationCaptor.getValue();
        assertThat(savedStation.getName()).isEqualTo("Stacja Nowa");
        assertThat(savedStation.getLatitude()).isEqualTo(52.1);
        assertThat(savedStation.getLongitude()).isEqualTo(21.1);
        assertThat(savedStation.getAddressLine()).isEqualTo("ul. Testowa 1");
        assertThat(savedStation.getCity()).isEqualTo("Warszawa");
        assertThat(savedStation.getCountry()).isEqualTo("PL");
        assertThat(savedStation.getExternalSource()).isEqualTo("MANUAL");
        assertThat(savedStation.getExternalId()).isNotBlank();
        assertThat(savedStation.isActive()).isTrue();
        assertThat(savedStation.getLastSyncedAt()).isNotNull();
        assertThat(savedStation.getAdminActiveLockedUntil()).isNotNull();
        assertThat(savedStation.getConnectors()).isEmpty();

        ArgumentCaptor<StationStatusSnapshot> snapshotCaptor = ArgumentCaptor.forClass(StationStatusSnapshot.class);
        verify(snapshotRepository).save(snapshotCaptor.capture());
        StationStatusSnapshot savedSnapshot = snapshotCaptor.getValue();
        assertThat(savedSnapshot.getStation()).isSameAs(savedStation);
        assertThat(savedSnapshot.getAvailableCount()).isEqualTo(0);
        assertThat(savedSnapshot.getSource()).isEqualTo("MANUAL");

        ArgumentCaptor<StationOwner> ownerCaptor = ArgumentCaptor.forClass(StationOwner.class);
        verify(stationOwnerRepository).save(ownerCaptor.capture());
        StationOwner savedOwnership = ownerCaptor.getValue();
        assertThat(savedOwnership.getStation()).isSameAs(savedStation);
        assertThat(savedOwnership.getOwner()).isSameAs(owner);

        assertThat(response.id()).isEqualTo(123L);
        assertThat(response.name()).isEqualTo("Stacja Nowa");
        assertThat(response.active()).isTrue();
        assertThat(response.latestStatus()).isNotNull();
        assertThat(response.latestStatus().availableCount()).isEqualTo(0);
    }

    @Test
    void createStation_persistsConnectorsAndUsesQuantityAsAvailable() {
        User owner = new User();
        owner.setId(11L);
        owner.setRole(UserRole.OWNER);

        CreateStationRequest request = new CreateStationRequest(
                "Stacja",
                52.0,
                21.0,
                null, null, null, null, null, null,
                List.of(
                        new ConnectorRequest("CCS2", "DC", new BigDecimal("50.0"), 2),
                        new ConnectorRequest("Type2", "AC", new BigDecimal("22.0"), 1)
                )
        );

        when(userRepository.findById(11L)).thenReturn(Optional.of(owner));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> simulatePersist(inv.getArgument(0), 150L));
        when(snapshotRepository.save(any(StationStatusSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stationOwnerRepository.save(any(StationOwner.class))).thenAnswer(inv -> {
            StationOwner so = inv.getArgument(0);
            if (so.getAssignedAt() == null) so.setAssignedAt(Instant.now());
            return so;
        });

        StationDetailsResponse response = service.createStation(request, 11L);

        ArgumentCaptor<Station> stationCaptor = ArgumentCaptor.forClass(Station.class);
        verify(stationRepository).save(stationCaptor.capture());
        Station savedStation = stationCaptor.getValue();
        assertThat(savedStation.getConnectors()).hasSize(2);
        assertThat(savedStation.getConnectors().get(0).getConnectorType()).isEqualTo("CCS2");
        assertThat(savedStation.getConnectors().get(0).getQuantity()).isEqualTo(2);
        assertThat(savedStation.getConnectors().get(0).getStation()).isSameAs(savedStation);

        ArgumentCaptor<StationStatusSnapshot> snapshotCaptor = ArgumentCaptor.forClass(StationStatusSnapshot.class);
        verify(snapshotRepository).save(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().getAvailableCount()).isEqualTo(3);

        assertThat(response.connectors()).hasSize(2);
    }

    private static Station simulatePersist(Station station, long stationId) {
        if (station.getId() == null) station.setId(stationId);
        long connectorIdCounter = 1000L;
        for (StationConnector c : station.getConnectors()) {
            if (c.getId() == null) c.setId(connectorIdCounter++);
        }
        for (StationOwner o : station.getOwners()) {
            if (o.getAssignedAt() == null) o.setAssignedAt(Instant.now());
        }
        return station;
    }

    @Test
    void createStation_allowsAdminToCreate() {
        User admin = new User();
        admin.setId(9L);
        admin.setRole(UserRole.ADMIN);
        CreateStationRequest request = new CreateStationRequest(
                "Stacja", 52.0, 21.0, null, null, null, null, null, null, null);

        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> simulatePersist(inv.getArgument(0), 200L));
        when(snapshotRepository.save(any(StationStatusSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stationOwnerRepository.save(any(StationOwner.class))).thenAnswer(inv -> {
            StationOwner so = inv.getArgument(0);
            if (so.getAssignedAt() == null) so.setAssignedAt(Instant.now());
            return so;
        });

        StationDetailsResponse response = service.createStation(request, 9L);

        assertThat(response.id()).isEqualTo(200L);
        verify(stationOwnerRepository).save(any(StationOwner.class));
    }

    @Test
    void requireWriteAccess_passesForAdmin() {
        service.requireWriteAccess(1L, 99L, UserRole.ADMIN);
        verifyNoInteractions(stationOwnerRepository);
    }

    @Test
    void requireWriteAccess_passesForOwnerOfStation() {
        when(stationOwnerRepository.existsByStationIdAndOwner_Id(1L, 7L)).thenReturn(true);

        service.requireWriteAccess(1L, 7L, UserRole.OWNER);

        verify(stationOwnerRepository).existsByStationIdAndOwner_Id(1L, 7L);
    }

    @Test
    void requireWriteAccess_throwsForbiddenForOwnerOfOtherStation() {
        when(stationOwnerRepository.existsByStationIdAndOwner_Id(2L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> service.requireWriteAccess(2L, 7L, UserRole.OWNER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireWriteAccess_throwsForbiddenForRegularUser() {
        assertThatThrownBy(() -> service.requireWriteAccess(1L, 7L, UserRole.USER))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(stationOwnerRepository);
    }

    @Test
    void updateStation_throwsNotFound_whenStationMissing() {
        UpdateStationRequest request = new UpdateStationRequest(
                "n", 1.0, 2.0, null, null, null, null, null, null, true, null);
        when(stationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStation(1L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateStation_persistsChangesAndReturnsDetails() {
        UpdateStationRequest request = new UpdateStationRequest(
                "Nowa nazwa", 51.0, 21.0, "Ul. Główna 1", "Warszawa",
                "PL", "Operator", "Mon-Fri", "private", false, null);

        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));
        when(snapshotRepository.findTopByStationIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(snapshot));

        StationDetailsResponse response = service.updateStation(1L, request);

        ArgumentCaptor<Station> captor = ArgumentCaptor.forClass(Station.class);
        verify(stationRepository).save(captor.capture());
        Station saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Nowa nazwa");
        assertThat(saved.getLatitude()).isEqualTo(51.0);
        assertThat(saved.getLongitude()).isEqualTo(21.0);
        assertThat(saved.getAddressLine()).isEqualTo("Ul. Główna 1");
        assertThat(saved.getCity()).isEqualTo("Warszawa");
        assertThat(saved.getCountry()).isEqualTo("PL");
        assertThat(saved.getOperatorName()).isEqualTo("Operator");
        assertThat(saved.getOpeningHours()).isEqualTo("Mon-Fri");
        assertThat(saved.getAccessType()).isEqualTo("private");
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.getLastSyncedAt()).isNotNull();
        // Connectors nie ruszone, gdy w request nie podano listy
        assertThat(saved.getConnectors()).hasSize(1);

        assertThat(response.name()).isEqualTo("Nowa nazwa");
        assertThat(response.active()).isFalse();
    }

    @Test
    void updateStation_replacesConnectorsWhenProvided() {
        UpdateStationRequest request = new UpdateStationRequest(
                "n", 1.0, 2.0, null, null, null, null, null, null, true,
                List.of(
                        new ConnectorRequest("Type2", "AC", new BigDecimal("11.0"), 3),
                        new ConnectorRequest("CHAdeMO", "DC", new BigDecimal("50.0"), 1)
                )
        );

        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> simulatePersist(inv.getArgument(0), 1L));
        when(snapshotRepository.findTopByStationIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(snapshot));

        service.updateStation(1L, request);

        ArgumentCaptor<Station> captor = ArgumentCaptor.forClass(Station.class);
        verify(stationRepository).save(captor.capture());
        Station saved = captor.getValue();
        assertThat(saved.getConnectors()).hasSize(2);
        assertThat(saved.getConnectors().get(0).getConnectorType()).isEqualTo("Type2");
        assertThat(saved.getConnectors().get(0).getQuantity()).isEqualTo(3);
        assertThat(saved.getConnectors().get(1).getConnectorType()).isEqualTo("CHAdeMO");
    }

    @Test
    void updateStation_replacesConnectorsWithEmptyListWhenProvided() {
        UpdateStationRequest request = new UpdateStationRequest(
                "n", 1.0, 2.0, null, null, null, null, null, null, true, List.of());

        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> simulatePersist(inv.getArgument(0), 1L));
        when(snapshotRepository.findTopByStationIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(snapshot));

        service.updateStation(1L, request);

        ArgumentCaptor<Station> captor = ArgumentCaptor.forClass(Station.class);
        verify(stationRepository).save(captor.capture());
        assertThat(captor.getValue().getConnectors()).isEmpty();
    }

    @Test
    void deleteStationById_throwsNotFound_whenMissing() {
        when(stationRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteStationById(99L))
                .isInstanceOf(NotFoundException.class);

        verify(stationRepository, never()).deleteById(any());
    }

    @Test
    void deleteStationById_deletesWhenExists() {
        when(stationRepository.existsById(1L)).thenReturn(true);

        service.deleteStationById(1L);

        verify(stationRepository).deleteById(1L);
    }
}
