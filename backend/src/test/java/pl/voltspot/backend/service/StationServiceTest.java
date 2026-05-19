package pl.voltspot.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.voltspot.backend.client.OCMClient;
import pl.voltspot.backend.dto.station.StationDetailsResponse;
import pl.voltspot.backend.dto.station.StationMarkerResponse;
import pl.voltspot.backend.dto.station.StationStatusSnapshotResponse;
import pl.voltspot.backend.dto.station.UpdateStationRequest;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationConnector;
import pl.voltspot.backend.entity.StationStatusSnapshot;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.StationStatusSnapshotRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    void getStations_withBbox_callsOcmAndReturnsDbResults() {
        when(ocmClient.fetchStations(any(), any(), any(), any())).thenReturn(List.of());
        when(stationRepository.findByActiveTrueAndLatitudeBetweenAndLongitudeBetweenOrderByIdAsc(
                49.9, 50.1, 19.9, 20.1)).thenReturn(List.of(station));
        when(snapshotRepository.findLatestForStations(anyList())).thenReturn(List.of(snapshot));

        List<StationMarkerResponse> result = service.getStations(49.9, 50.1, 19.9, 20.1);

        assertThat(result).hasSize(1);
        verify(ocmClient, times(1)).fetchStations(49.9, 19.9, 50.1, 20.1);
        verify(stationRepository).findByActiveTrueAndLatitudeBetweenAndLongitudeBetweenOrderByIdAsc(
                49.9, 50.1, 19.9, 20.1);
    }

    @Test
    void getStations_withBbox_swallowsOcmFailureAndStillReturnsCache() {
        when(ocmClient.fetchStations(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("ocm offline"));
        when(stationRepository.findByActiveTrueAndLatitudeBetweenAndLongitudeBetweenOrderByIdAsc(
                49.9, 50.1, 19.9, 20.1)).thenReturn(List.of(station));
        when(snapshotRepository.findLatestForStations(anyList())).thenReturn(List.of(snapshot));

        List<StationMarkerResponse> result = service.getStations(49.9, 50.1, 19.9, 20.1);

        assertThat(result).hasSize(1);
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
    void updateStation_throwsNotFound_whenStationMissing() {
        UpdateStationRequest request = new UpdateStationRequest(
                "n", 1.0, 2.0, null, null, null, null, null, null, true);
        when(stationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStation(1L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateStation_persistsChangesAndReturnsDetails() {
        UpdateStationRequest request = new UpdateStationRequest(
                "Nowa nazwa", 51.0, 21.0, "Ul. Główna 1", "Warszawa",
                "PL", "Operator", "Mon-Fri", "private", false);

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

        assertThat(response.name()).isEqualTo("Nowa nazwa");
        assertThat(response.active()).isFalse();
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
