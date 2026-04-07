package pl.voltspot.backend.dto.external;

import java.util.List;

public record ExternalOCMStation(
        Long id,
        String uuid,
        Integer operatorId,
        AddressInfo addressInfo,
        List<Connection> connections,
        String dateLastStatusUpdate,
        StatusType statusType,
        List<UserComment> userComments
) {
    public record AddressInfo(
            Long id,
            String title,
            String addressLine1,
            String town,
            Double latitude,
            Double longitude,
            Integer countryId
    ) {}

    public record Connection(
            Long id,
            Integer connectionTypeId,
            Double powerKW,
            Integer currentTypeId,
            Integer quantity,
            Integer statusTypeId
            //Co to jest externalConnectorKey....
    ) {}

    public record StatusType(
            Boolean isOperational,
            Integer id,
            String title
    ) {}

    public record UserComment(
            Long id,
            Integer chargePointId,
            Integer commentTypeId,
            String userName,
            String comment
    ) {}
}