package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.voltspot.backend.entity.UserFavorite;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    List<UserFavorite> findByUserId(Long userId);

    Optional<UserFavorite> findByUserIdAndStationId(Long userId, Long stationId);

    boolean existsByUserIdAndStationId(Long userId, Long stationId);

    void deleteByUserIdAndStationId(Long userId, Long stationId);
}
