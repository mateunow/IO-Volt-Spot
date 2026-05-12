package pl.voltspot.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.voltspot.backend.auth.RequireAuth;
import pl.voltspot.backend.dto.favorite.UserFavoriteResponse;
import pl.voltspot.backend.service.UserFavoriteService;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class UserFavoriteController {

    private final UserFavoriteService userFavoriteService;

    @PostMapping("/{stationId}")
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    public UserFavoriteResponse addFavorite(@PathVariable Long stationId) {
        return userFavoriteService.addFavorite(stationId);
    }

    @DeleteMapping("/{stationId}")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(@PathVariable Long stationId) {
        userFavoriteService.removeFavorite(stationId);
    }

    @GetMapping
    public List<UserFavoriteResponse> getFavorites() {
        return userFavoriteService.getUserFavorites();
    }

    @GetMapping("/{stationId}/is-favorited")
    public boolean isFavorited(@PathVariable Long stationId) {
        return userFavoriteService.isFavorited(stationId);
    }
}
