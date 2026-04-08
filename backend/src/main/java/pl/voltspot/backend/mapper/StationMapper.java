package pl.voltspot.backend.mapper;

import pl.voltspot.backend.dto.external.ExternalOCMStation;
import pl.voltspot.backend.dto.feedback.StationFeedbackResponse;
import pl.voltspot.backend.dto.station.*;
import pl.voltspot.backend.dto.user.UserResponse;
import pl.voltspot.backend.entity.*;
import pl.voltspot.backend.entity.User;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static pl.voltspot.backend.mapper.ConnectionTypeID.CONNECTORS;
import static pl.voltspot.backend.mapper.CountryID.COUNTRIES;
import static pl.voltspot.backend.mapper.CurrentTypeID.CURRENTS;

public final class StationMapper {

    private StationMapper() {
    }

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

    public static StationMarkerResponse toMarkerResponse(Station station) {
        return new StationMarkerResponse(
                station.getId(),
                station.getName(),
                station.getLatitude(),
                station.getLongitude(),
                station.getCity(),
                station.getOperatorName()
        );
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

    public static Station toEntity(ExternalOCMStation external){
        if(external == null) return null;
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
                .orElse("Unknown")
        );

        station.setOperatorName(
                Optional.ofNullable(external.operatorInfo())
                        .map(ExternalOCMStation.OperatorInfo::title)
                        .orElse("Unknown")
        );

        station.setActive(
                Optional.ofNullable(external.statusType())
                        .map(ExternalOCMStation.StatusType::isOperational)
                        .orElse(true)
        );

        if(external.connections() != null){
            for(ExternalOCMStation.Connection connection : external.connections()){
                StationConnector connector = new StationConnector();
                connector.setStation(station);
                if(connection.connectionTypeId() != null){
                    connector.setConnectorType(
                            CONNECTORS.getOrDefault(connection.connectionTypeId(), "Unknown")
                    );
                } else {
                    connector.setConnectorType("Unknown");
                }
                if(connection.currentTypeId() != null){
                    connector.setCurrentType(
                            CURRENTS.getOrDefault(connection.currentTypeId(), "Unknown")
                    );
                }
                if(connection.powerKW() != null){
                    connector.setPowerKw(new BigDecimal(connection.powerKW()));
                }
                if(connection.quantity() != null){
                    connector.setQuantity(connection.quantity());
                } else {
                    connector.setQuantity(1);
                }

                station.getConnectors().add(connector);
            }
        }

        return station;
    }
}