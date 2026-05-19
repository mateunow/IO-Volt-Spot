package pl.voltspot.backend.mapper;

import org.junit.jupiter.api.Test;
import pl.voltspot.backend.dto.favorite.UserFavoriteResponse;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.UserFavorite;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserFavoriteMapperTest {

    @Test
    void toUserFavoriteResponse_returnsNull_whenFavoriteIsNull() {
        assertThat(UserFavoriteMapper.toUserFavoriteResponse(null)).isNull();
    }

    @Test
    void toUserFavoriteResponse_returnsNull_whenStationIsNull() {
        UserFavorite favorite = new UserFavorite();
        favorite.setId(1L);

        assertThat(UserFavoriteMapper.toUserFavoriteResponse(favorite)).isNull();
    }

    @Test
    void toUserFavoriteResponse_mapsAllFields_whenStationIsPresent() {
        Station station = new Station();
        station.setId(10L);
        station.setName("Stacja A");
        station.setCity("Kraków");
        station.setLatitude(50.0);
        station.setLongitude(20.0);

        UserFavorite favorite = new UserFavorite();
        favorite.setId(1L);
        favorite.setStation(station);
        favorite.setAddedAt(Instant.parse("2025-05-01T10:00:00Z"));

        UserFavoriteResponse response = UserFavoriteMapper.toUserFavoriteResponse(favorite);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.stationId()).isEqualTo(10L);
        assertThat(response.stationName()).isEqualTo("Stacja A");
        assertThat(response.city()).isEqualTo("Kraków");
        assertThat(response.latitude()).isEqualTo(50.0);
        assertThat(response.longitude()).isEqualTo(20.0);
        assertThat(response.addedAt()).isEqualTo(Instant.parse("2025-05-01T10:00:00Z"));
    }
}
