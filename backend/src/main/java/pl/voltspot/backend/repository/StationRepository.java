package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.voltspot.backend.entity.Station;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findByActiveTrueOrderByIdAsc();

    List<Station> findByActiveTrueAndLatitudeBetweenAndLongitudeBetweenOrderByIdAsc(
            Double minLat,
            Double maxLat,
            Double minLon,
            Double maxLon
    );

    Optional<Station> findByExternalSourceAndExternalId(String externalSource, String externalId);
}