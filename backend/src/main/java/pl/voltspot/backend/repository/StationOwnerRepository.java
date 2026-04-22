package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.voltspot.backend.entity.StationOwner;

import java.util.List;

public interface StationOwnerRepository extends JpaRepository<StationOwner, Long> {

    boolean existsByStationIdAndOwner_Id(Long stationId, Long ownerId);

    List<StationOwner> findByStationIdOrderByAssignedAtAsc(Long stationId);

    List<StationOwner> findByOwner_IdOrderByAssignedAtAsc(Long ownerId);
}