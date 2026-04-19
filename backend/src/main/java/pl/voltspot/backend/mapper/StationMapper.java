package pl.voltspot.backend.mapper;

import pl.voltspot.backend.dto.external.ExternalOCMStation;
import pl.voltspot.backend.dto.feedback.StationFeedbackResponse;
import pl.voltspot.backend.dto.station.*;
import pl.voltspot.backend.dto.user.UserResponse;
import pl.voltspot.backend.entity.*;
import pl.voltspot.backend.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static pl.voltspot.backend.mapper.ConnectionTypeID.CONNECTORS;
import static pl.voltspot.backend.mapper.CountryID.COUNTRIES;
import static pl.voltspot.backend.mapper.CurrentTypeID.CURRENTS;

public final class StationMapper {

    private StationMapper() {}
    public record StationWithSnapshot(Station station, StationStatusSnapshot snapshot) {}

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    public static StationMarkerResponse toMarkerResponse(Station station, StationStatusSnapshot snapshot) {
        return new StationMarkerResponse(
                station.getId(),
                station.getName(),
                station.getLatitude(),
                station.getLongitude(),
                station.getCity(),
                station.getOperatorName(),
                resolveMarkerStatus(snapshot)
        );
    }

    /**
     * Na podstawie snapshotu wyznacza kategorię ikony markera:
     * - WORKING  – są dostępne gniazda
     * - OCCUPIED – zajęte/chwilowo niedostępne, brak wolnych
     * - DISABLED – wyłączona lub usunięta
     * - DEFAULT  – brak snapshotu lub same zera (status nieznany)
     */
    private static String resolveMarkerStatus(StationStatusSnapshot s) {
        if (s == null) return "DEFAULT";

        int available    = s.getAvailableCount()    != null ? s.getAvailableCount()    : 0;
        int occupied     = s.getOccupiedCount()     != null ? s.getOccupiedCount()     : 0;
        int outOfService = s.getOutOfServiceCount() != null ? s.getOutOfServiceCount() : 0;
        int reserved     = s.getReservedCount()     != null ? s.getReservedCount()     : 0;

        if (available > 0) return "WORKING";
        if (outOfService > 0 && occupied == 0 && available == 0 && reserved == 0) return "DISABLED";
        if (occupied > 0 || reserved > 0) return "OCCUPIED";
        return "DEFAULT";
    }

    public static StationConnectorResponse toConnectorResponse(StationConnector connector) {
        return new StationConnectorResponse(
                connector.getId(),
                connector.getConnectorType(),
                connector.getCurrentType(),
                connector.getPowerKw(),
                connector.getQuantity()
        );
    }

    public static StationStatusSnapshotResponse toStatusResponse(StationStatusSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new StationStatusSnapshotResponse(
                snapshot.getId(),
                snapshot.getSource(),
                snapshot.getAvailableCount(),
                snapshot.getOccupiedCount(),
                snapshot.getReservedCount(),
                snapshot.getOutOfServiceCount(),
                snapshot.getUnknownCount(),
                snapshot.getRecordedAt()
        );
    }

    public static StationOwnerResponse toOwnerResponse(StationOwner stationOwner) {
        return new StationOwnerResponse(
                stationOwner.getId(),
                stationOwner.getOwner().getId(),
                stationOwner.getOwner().getDisplayName(),
                stationOwner.getOwner().getEmail(),
                stationOwner.getAssignedAt()
        );
    }

    public static StationDetailsResponse toDetailsResponse(Station station, StationStatusSnapshot latestStatus) {
        List<StationConnectorResponse> connectors = station.getConnectors()
                .stream()
                .sorted(Comparator.comparing(StationConnector::getId))
                .map(StationMapper::toConnectorResponse)
                .toList();

        List<StationOwnerResponse> owners = station.getOwners()
                .stream()
                .sorted(Comparator.comparing(StationOwner::getAssignedAt))
                .map(StationMapper::toOwnerResponse)
                .toList();

        return new StationDetailsResponse(
                station.getId(),
                station.getExternalSource(),
                station.getExternalId(),
                station.getName(),
                station.getLatitude(),
                station.getLongitude(),
                station.getAddressLine(),
                station.getCity(),
                station.getCountry(),
                station.getOperatorName(),
                station.getOpeningHours(),
                station.getAccessType(),
                station.isActive(),
                station.getLastSyncedAt(),
                connectors,
                toStatusResponse(latestStatus),
                owners
        );
    }

    public static StationFeedbackResponse toFeedbackResponse(StationFeedback feedback) {
        return new StationFeedbackResponse(
                feedback.getId(),
                feedback.getStation().getId(),
                feedback.getUser().getId(),
                feedback.getUser().getDisplayName(),
                feedback.getOperationalStatus(),
                feedback.getComment(),
                feedback.getCreatedAt()
        );
    }

    /**
     * Mapuje zewnętrzną stację OCM na parę (Station, StationStatusSnapshot).
     * Snapshot jest tworzony zawsze – jeśli OCM nie ma danych o statusach złączy,
     * używany jest status stacji-level (statusType.id).
     */
    public static StationWithSnapshot toEntityWithSnapshot(ExternalOCMStation external) {
        if (external == null) return null;

        Station station = new Station();
        station.setExternalId(external.id().toString());
        station.setExternalSource("OCM");

        var addr = Optional.ofNullable(external.addressInfo());
        station.setName(addr.map(ExternalOCMStation.AddressInfo::title).orElse("Unknown Station"));
        station.setLatitude(addr.map(ExternalOCMStation.AddressInfo::latitude).orElse(0.0));
        station.setLongitude(addr.map(ExternalOCMStation.AddressInfo::longitude).orElse(0.0));
        station.setAddressLine(addr.map(ExternalOCMStation.AddressInfo::addressLine1).orElse(null));
        station.setCity(addr.map(ExternalOCMStation.AddressInfo::town).orElse(""));
        station.setCountry(addr
                .map(ExternalOCMStation.AddressInfo::countryId)
                .map(id -> COUNTRIES.getOrDefault(id, "Unknown"))
                .orElse("Unknown"));

        station.setOperatorName(
                Optional.ofNullable(external.operatorInfo())
                        .map(ExternalOCMStation.OperatorInfo::title)
                        .orElse("Unknown"));

        boolean isOperational = Optional.ofNullable(external.statusType())
                .map(ExternalOCMStation.StatusType::isOperational)
                .orElse(true);
        station.setActive(isOperational);

        int available    = 0;
        int occupied     = 0;
        int reserved     = 0;
        int outOfService = 0;
        int unknown      = 0;

        List<ExternalOCMStation.Connection> connections =
                external.connections() != null ? external.connections() : List.of();

        for (ExternalOCMStation.Connection connection : connections) {
            StationConnector connector = new StationConnector();
            connector.setStation(station);

            connector.setConnectorType(connection.connectionTypeId() != null
                    ? CONNECTORS.getOrDefault(connection.connectionTypeId(), "Unknown")
                    : "Unknown");

            if (connection.currentTypeId() != null) {
                connector.setCurrentType(CURRENTS.getOrDefault(connection.currentTypeId(), "Unknown"));
            }
            if (connection.powerKW() != null) {
                connector.setPowerKw(BigDecimal.valueOf(connection.powerKW()));
            }
            connector.setQuantity(connection.quantity() != null ? connection.quantity() : 1);

            station.getConnectors().add(connector);
        }

        if (external.statusType() != null) {
            int totalConnectors = connections.stream()
                    .mapToInt(c -> c.quantity() != null ? c.quantity() : 1)
                    .sum();
            if (totalConnectors == 0) totalConnectors = 1;

            Integer stationStatusId = external.statusType().id();
            Boolean isOp = external.statusType().isOperational();

            if (stationStatusId != null) {
                switch (stationStatusId) {
                    case 10  -> available    = totalConnectors; // Currently Available
                    case 20  -> occupied     = totalConnectors; // Currently In Use
                    case 30  -> reserved     = totalConnectors; // Temporarily Unavailable
                    case 50  -> available    = totalConnectors; // Operational
                    case 75  -> {                               // Partly Operational
                        available    = Math.max(1, totalConnectors / 2);
                        outOfService = totalConnectors - available;
                    }
                    case 100, 150, 200, 210 -> outOfService = totalConnectors; // Not operational / Removed
                    default  -> unknown      = totalConnectors; // 0 = Unknown
                }
            } else if (Boolean.TRUE.equals(isOp)) {
                available = totalConnectors;
            } else if (Boolean.FALSE.equals(isOp)) {
                outOfService = totalConnectors;
            } else {
                unknown = totalConnectors;
            }
        }

        StationStatusSnapshot snapshot = new StationStatusSnapshot();
        snapshot.setStation(station);
        snapshot.setSource("OCM");
        snapshot.setAvailableCount(available);
        snapshot.setOccupiedCount(occupied);
        snapshot.setReservedCount(reserved);
        snapshot.setOutOfServiceCount(outOfService);
        snapshot.setUnknownCount(unknown);
        snapshot.setRecordedAt(Instant.now());

        return new StationWithSnapshot(station, snapshot);
    }
}
