package pl.voltspot.backend.mapper;

import pl.voltspot.backend.dto.favorite.UserFavoriteResponse;
import pl.voltspot.backend.entity.UserFavorite;

public final class UserFavoriteMapper {

    private UserFavoriteMapper() {}

    public static UserFavoriteResponse toUserFavoriteResponse(UserFavorite favorite) {
        if (favorite == null) {
            return null;
        }

        var station = favorite.getStation();
        if (station == null) {
            return null;
        }

        return new UserFavoriteResponse(
                favorite.getId(),
                station.getId(),
                station.getName(),
                station.getCity(),
                station.getLatitude(),
                station.getLongitude(),
                favorite.getAddedAt()
        );
    }
}
