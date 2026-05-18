package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.voltspot.backend.entity.CommunityStatusOverride;
import pl.voltspot.backend.enums.OverrideState;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface CommunityStatusOverrideRepository extends JpaRepository<CommunityStatusOverride, Long> {

    Optional<CommunityStatusOverride> findByStationIdAndStateIn(Long stationId, List<OverrideState> states);

    List<CommunityStatusOverride> findByStateOrderByCreatedAtDesc(OverrideState state);

    @Query("SELECT o FROM CommunityStatusOverride o WHERE o.state = 'CONFIRMED' AND o.expiresAt < :now")
    List<CommunityStatusOverride> findExpiredConfirmed(Instant now);

    @Query("SELECT o FROM CommunityStatusOverride o WHERE o.state = 'PENDING' AND o.createdAt < :cutoff")
    List<CommunityStatusOverride> findStalePending(Instant cutoff);

    @Query("SELECT o FROM CommunityStatusOverride o JOIN FETCH o.station WHERE o.state IN ('PENDING', 'CONFIRMED')")
    List<CommunityStatusOverride> findAllActive();

    default Map<Long, CommunityStatusOverride> findActiveByStationIds(List<Long> stationIds) {
        return findAllActive().stream()
                .filter(o -> stationIds.contains(o.getStation().getId()))
                .collect(Collectors.toMap(
                        o -> o.getStation().getId(),
                        o -> o,
                        (a, b) -> a
                ));
    }
}
