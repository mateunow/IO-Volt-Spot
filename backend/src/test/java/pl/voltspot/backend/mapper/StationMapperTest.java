package pl.voltspot.backend.mapper;

import org.junit.jupiter.api.Test;
import pl.voltspot.backend.dto.external.ExternalOCMStation;
import pl.voltspot.backend.dto.feedback.StationFeedbackResponse;
import pl.voltspot.backend.dto.station.StationDetailsResponse;
import pl.voltspot.backend.dto.station.StationMarkerResponse;
import pl.voltspot.backend.dto.station.StationStatusSnapshotResponse;
import pl.voltspot.backend.dto.user.UserResponse;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationConnector;
import pl.voltspot.backend.entity.StationFeedback;
import pl.voltspot.backend.entity.StationOwner;
import pl.voltspot.backend.entity.StationStatusSnapshot;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.OperationalStatus;
import pl.voltspot.backend.enums.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StationMapperTest {

    private User sampleUser(long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("u" + id + "@example.com");
        u.setDisplayName("User " + id);
        u.setRole(UserRole.USER);
        u.setActive(true);
        u.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        return u;
    }

    private Station sampleStation() {
        Station s = new Station();
        s.setId(1L);
        s.setExternalSource("OCM");
        s.setExternalId("ext-1");
        s.setName("Stacja A");
        s.setLatitude(50.0);
        s.setLongitude(20.0);
        s.setCity("Kraków");
        s.setOperatorName("Op");
        s.setOpeningHours("24/7");
        s.setAccessType("public");
        s.setActive(true);
        s.setLastSyncedAt(Instant.parse("2025-05-01T00:00:00Z"));
        return s;
    }

    private StationConnector sampleConnector(long id, String type, double powerKw) {
        StationConnector c = new StationConnector();
        c.setId(id);
        c.setConnectorType(type);
        c.setCurrentType("DC");
        c.setPowerKw(BigDecimal.valueOf(powerKw));
        c.setQuantity(1);
        return c;
    }

    @Test
    void toUserResponse_mapsAllFields() {
        User user = sampleUser(5L);

        UserResponse response = StationMapper.toUserResponse(user);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.email()).isEqualTo("u5@example.com");
        assertThat(response.displayName()).isEqualTo("User 5");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
    }

    @Test
    void toMarkerResponse_returnsWorking_whenAvailableCountIsPositive() {
        Station s = sampleStation();
        s.getConnectors().add(sampleConnector(1L, "CCS", 50.0));
        s.getConnectors().add(sampleConnector(2L, "Type 2", 22.0));

        StationStatusSnapshot snapshot = new StationStatusSnapshot();
        snapshot.setAvailableCount(1);
        snapshot.setOccupiedCount(0);
        snapshot.setReservedCount(0);
        snapshot.setOutOfServiceCount(0);

        StationMarkerResponse response = StationMapper.toMarkerResponse(s, snapshot);

        assertThat(response.markerStatus()).isEqualTo("WORKING");
        assertThat(response.connectorTypes()).containsExactly("CCS", "Type 2");
        assertThat(response.maxPowerKw()).isEqualTo(50.0);
    }

    @Test
    void toMarkerResponse_returnsOccupied_whenOnlyOccupiedAndReserved() {
        Station s = sampleStation();
        StationStatusSnapshot snapshot = new StationStatusSnapshot();
        snapshot.setAvailableCount(0);
        snapshot.setOccupiedCount(1);
        snapshot.setReservedCount(0);
        snapshot.setOutOfServiceCount(0);

        StationMarkerResponse response = StationMapper.toMarkerResponse(s, snapshot);

        assertThat(response.markerStatus()).isEqualTo("OCCUPIED");
    }

    @Test
    void toMarkerResponse_returnsDisabled_whenAllOutOfService() {
        Station s = sampleStation();
        StationStatusSnapshot snapshot = new StationStatusSnapshot();
        snapshot.setAvailableCount(0);
        snapshot.setOccupiedCount(0);
        snapshot.setReservedCount(0);
        snapshot.setOutOfServiceCount(2);

        StationMarkerResponse response = StationMapper.toMarkerResponse(s, snapshot);

        assertThat(response.markerStatus()).isEqualTo("DISABLED");
    }

    @Test
    void toMarkerResponse_returnsDefault_whenSnapshotIsNullOrEmpty() {
        Station s = sampleStation();

        StationMarkerResponse withoutSnapshot = StationMapper.toMarkerResponse(s, null);
        assertThat(withoutSnapshot.markerStatus()).isEqualTo("DEFAULT");

        StationStatusSnapshot zeroSnapshot = new StationStatusSnapshot();
        zeroSnapshot.setAvailableCount(0);
        zeroSnapshot.setOccupiedCount(0);
        zeroSnapshot.setReservedCount(0);
        zeroSnapshot.setOutOfServiceCount(0);
        zeroSnapshot.setUnknownCount(0);

        StationMarkerResponse withZero = StationMapper.toMarkerResponse(s, zeroSnapshot);
        assertThat(withZero.markerStatus()).isEqualTo("DEFAULT");
    }

    @Test
    void toMarkerResponse_excludesUnknownConnectorTypes() {
        Station s = sampleStation();
        s.getConnectors().add(sampleConnector(1L, "CCS", 50.0));
        s.getConnectors().add(sampleConnector(2L, "Unknown", 22.0));
        s.getConnectors().add(sampleConnector(3L, "CCS", 50.0));

        StationMarkerResponse response = StationMapper.toMarkerResponse(s, null);

        assertThat(response.connectorTypes()).containsExactly("CCS");
    }

    @Test
    void toMarkerResponse_returnsNullMaxPower_whenNoConnectors() {
        Station s = sampleStation();

        StationMarkerResponse response = StationMapper.toMarkerResponse(s, null);

        assertThat(response.maxPowerKw()).isNull();
        assertThat(response.connectorTypes()).isEmpty();
    }

    @Test
    void toStatusResponse_returnsNull_whenSnapshotIsNull() {
        assertThat(StationMapper.toStatusResponse(null)).isNull();
    }

    @Test
    void toStatusResponse_mapsAllFields() {
        StationStatusSnapshot s = new StationStatusSnapshot();
        s.setId(7L);
        s.setSource("OCM");
        s.setAvailableCount(1);
        s.setOccupiedCount(2);
        s.setReservedCount(3);
        s.setOutOfServiceCount(4);
        s.setUnknownCount(5);
        s.setRecordedAt(Instant.parse("2025-05-01T10:00:00Z"));

        StationStatusSnapshotResponse response = StationMapper.toStatusResponse(s);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.source()).isEqualTo("OCM");
        assertThat(response.availableCount()).isEqualTo(1);
        assertThat(response.occupiedCount()).isEqualTo(2);
        assertThat(response.reservedCount()).isEqualTo(3);
        assertThat(response.outOfServiceCount()).isEqualTo(4);
        assertThat(response.unknownCount()).isEqualTo(5);
        assertThat(response.recordedAt()).isEqualTo(Instant.parse("2025-05-01T10:00:00Z"));
    }

    @Test
    void toDetailsResponse_sortsConnectorsAndOwners() {
        Station s = sampleStation();
        StationConnector c2 = sampleConnector(2L, "CCS", 50);
        StationConnector c1 = sampleConnector(1L, "Type 2", 22);
        s.getConnectors().add(c2);
        s.getConnectors().add(c1);

        StationOwner o1 = new StationOwner();
        o1.setId(100L);
        o1.setOwner(sampleUser(10L));
        o1.setStation(s);
        o1.setAssignedAt(Instant.parse("2025-02-01T00:00:00Z"));

        StationOwner o2 = new StationOwner();
        o2.setId(101L);
        o2.setOwner(sampleUser(11L));
        o2.setStation(s);
        o2.setAssignedAt(Instant.parse("2025-01-01T00:00:00Z"));

        s.getOwners().addAll(List.of(o1, o2));

        StationDetailsResponse response = StationMapper.toDetailsResponse(s, null);

        assertThat(response.connectors()).extracting("id").containsExactly(1L, 2L);
        assertThat(response.owners()).extracting("ownerId").containsExactly(11L, 10L);
        assertThat(response.latestStatus()).isNull();
    }

    @Test
    void toFeedbackResponse_mapsAllFields() {
        Station s = sampleStation();
        User author = sampleUser(7L);

        StationFeedback feedback = new StationFeedback();
        feedback.setId(99L);
        feedback.setStation(s);
        feedback.setUser(author);
        feedback.setOperationalStatus(OperationalStatus.BUSY);
        feedback.setComment("kolejka");
        feedback.setCreatedAt(Instant.parse("2025-05-01T00:00:00Z"));

        StationFeedbackResponse response = StationMapper.toFeedbackResponse(feedback);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.stationId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.userDisplayName()).isEqualTo("User 7");
        assertThat(response.operationalStatus()).isEqualTo(OperationalStatus.BUSY);
        assertThat(response.comment()).isEqualTo("kolejka");
    }

    @Test
    void toEntityWithSnapshot_returnsNull_forNullInput() {
        assertThat(StationMapper.toEntityWithSnapshot(null)).isNull();
    }

    @Test
    void toEntityWithSnapshot_mapsBasicStationFields() {
        ExternalOCMStation external = new ExternalOCMStation(
                123L,
                "uuid-1",
                new ExternalOCMStation.AddressInfo(1L, "Tytuł", "Ulica 1", "Kraków", 50.1, 19.9, null),
                List.of(),
                "2025-05-01T00:00:00Z",
                new ExternalOCMStation.StatusType(true, 10, "Currently Available"),
                List.of(),
                new ExternalOCMStation.OperatorInfo("Operator A")
        );

        StationMapper.StationWithSnapshot pair = StationMapper.toEntityWithSnapshot(external);

        assertThat(pair).isNotNull();
        Station station = pair.station();
        assertThat(station.getExternalSource()).isEqualTo("OCM");
        assertThat(station.getExternalId()).isEqualTo("123");
        assertThat(station.getName()).isEqualTo("Tytuł");
        assertThat(station.getLatitude()).isEqualTo(50.1);
        assertThat(station.getLongitude()).isEqualTo(19.9);
        assertThat(station.getCity()).isEqualTo("Kraków");
        assertThat(station.getOperatorName()).isEqualTo("Operator A");
        assertThat(station.isActive()).isTrue();

        StationStatusSnapshot snapshot = pair.snapshot();
        assertThat(snapshot.getSource()).isEqualTo("OCM");
        assertThat(snapshot.getAvailableCount()).isPositive();
    }

    @Test
    void toEntityWithSnapshot_marksStationAsOutOfService_forStatus100() {
        ExternalOCMStation external = new ExternalOCMStation(
                7L,
                "uuid-7",
                new ExternalOCMStation.AddressInfo(null, "T", null, "X", 0.0, 0.0, null),
                List.of(new ExternalOCMStation.Connection(1L, null, 22.0, null, 3, null)),
                null,
                new ExternalOCMStation.StatusType(false, 100, "Not operational"),
                null,
                null
        );

        StationMapper.StationWithSnapshot pair = StationMapper.toEntityWithSnapshot(external);

        assertThat(pair.station().isActive()).isFalse();
        assertThat(pair.snapshot().getOutOfServiceCount()).isEqualTo(3);
        assertThat(pair.snapshot().getAvailableCount()).isZero();
    }

    @Test
    void toEntityWithSnapshot_setsUnknownDefaults_whenNothingProvided() {
        ExternalOCMStation external = new ExternalOCMStation(
                42L, "uuid-42",
                null, null, null, null, null, null);

        StationMapper.StationWithSnapshot pair = StationMapper.toEntityWithSnapshot(external);

        assertThat(pair.station().getName()).isEqualTo("Unknown Station");
        assertThat(pair.station().getCity()).isEmpty();
        assertThat(pair.station().getCountry()).isEqualTo("Unknown");
        assertThat(pair.station().getOperatorName()).isEqualTo("Unknown");
        assertThat(pair.station().isActive()).isTrue();
    }
}
