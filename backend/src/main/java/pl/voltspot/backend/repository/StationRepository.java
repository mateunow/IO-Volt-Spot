package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.voltspot.backend.entity.Station;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    @Query("""
            SELECT s FROM Station s
            LEFT JOIN FETCH s.connectors
            WHERE s.active = true
            ORDER BY s.id ASC
            """)
    List<Station> findByActiveTrueOrderByIdAsc();

    @Query("""
            SELECT s FROM Station s
            LEFT JOIN FETCH s.connectors
            WHERE s.active = true
              AND s.latitude  BETWEEN :minLat AND :maxLat
              AND s.longitude BETWEEN :minLon AND :maxLon
            ORDER BY s.id ASC
            """)
    List<Station> findByActiveTrueAndLatitudeBetweenAndLongitudeBetweenOrderByIdAsc(
            Double minLat,
            Double maxLat,
            Double minLon,
            Double maxLon
    );

    Optional<Station> findByExternalSourceAndExternalId(String externalSource, String externalId);
}