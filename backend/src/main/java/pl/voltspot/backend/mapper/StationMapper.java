package pl.voltspot.backend.mapper;

import pl.voltspot.backend.dto.feedback.StationFeedbackResponse;
import pl.voltspot.backend.dto.station.*;
import pl.voltspot.backend.dto.user.UserResponse;
import pl.voltspot.backend.entity.*;
import pl.voltspot.backend.entity.User;

import java.util.Comparator;
import java.util.List;

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
}