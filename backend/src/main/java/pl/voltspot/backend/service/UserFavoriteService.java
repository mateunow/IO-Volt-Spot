package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.auth.AuthContext;
import pl.voltspot.backend.dto.favorite.UserFavoriteResponse;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.entity.UserFavorite;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.exceptions.UnauthorizedException;
import pl.voltspot.backend.mapper.UserFavoriteMapper;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.UserFavoriteRepository;
import pl.voltspot.backend.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserFavoriteService {

    private static final Logger log = LoggerFactory.getLogger(UserFavoriteService.class);

    private final UserFavoriteRepository userFavoriteRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;

    public UserFavoriteResponse addFavorite(Long stationId) {
        var currentUser = AuthContext.get();
        if (currentUser == null) {
            throw new UnauthorizedException("Musisz być zalogowany");
        }

        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("Użytkownik nie znaleziony"));

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Stacja nie znaleziona"));

        if (userFavoriteRepository.existsByUserIdAndStationId(user.getId(), stationId)) {
            throw new BadRequestException("Ta stacja jest już w ulubionych");
        }

        UserFavorite favorite = new UserFavorite();
        favorite.setUser(user);
        favorite.setStation(station);

        UserFavorite saved = userFavoriteRepository.save(favorite);
        log.info("Dodano ulubioną stację {} dla użytkownika {}", stationId, user.getId());

        return UserFavoriteMapper.toUserFavoriteResponse(saved);
    }

    public void removeFavorite(Long stationId) {
        var currentUser = AuthContext.get();
        if (currentUser == null) {
            throw new UnauthorizedException("Musisz być zalogowany");
        }

        userFavoriteRepository.deleteByUserIdAndStationId(currentUser.id(), stationId);
        log.info("Usunięto ulubioną stację {} dla użytkownika {}", stationId, currentUser.id());
    }

    @Transactional(readOnly = true)
    public List<UserFavoriteResponse> getUserFavorites() {
        var currentUser = AuthContext.get();
        if (currentUser == null) {
            return List.of();
        }

        List<UserFavorite> favorites = userFavoriteRepository.findByUserId(currentUser.id());
        return favorites.stream()
                .map(UserFavoriteMapper::toUserFavoriteResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(Long stationId) {
        var currentUser = AuthContext.get();
        if (currentUser == null) {
            return false;
        }

        return userFavoriteRepository.existsByUserIdAndStationId(currentUser.id(), stationId);
    }
}
