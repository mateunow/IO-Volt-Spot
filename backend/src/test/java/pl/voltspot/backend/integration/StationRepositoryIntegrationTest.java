package pl.voltspot.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationStatusSnapshot;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.StationStatusSnapshotRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


class StationRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private StationStatusSnapshotRepository snapshotRepository;

    @Test
    void findByActiveTrueOrderByIdAsc_returnsSeededStations() {
        List<Station> stations = stationRepository.findByActiveTrueOrderByIdAsc();

        assertThat(stations).extracting(Station::getId).containsExactly(1L, 2L, 3L);
        assertThat(stations.getFirst().getConnectors()).isNotEmpty();
    }

    @Test
    void findByActiveTrueAndLatitudeBetweenAndLongitudeBetween_filtersByBbox() {
        List<Station> result = stationRepository
                .findByActiveTrueAndLatitudeBetweenAndLongitudeBetweenOrderByIdAsc(
                        50.04, 50.06, 19.95, 19.97);

        assertThat(result).extracting(Station::getId).containsExactly(1L);
    }

    @Test
    void findByActiveTrueAndLatitudeBetweenAndLongitudeBetween_returnsEmptyForDisjointBox() {
        List<Station> result = stationRepository
                .findByActiveTrueAndLatitudeBetweenAndLongitudeBetweenOrderByIdAsc(
                        0.0, 1.0, 0.0, 1.0);

        assertThat(result).isEmpty();
    }

    @Test
    void findByActiveTrueOrderByIdAsc_excludesInactiveStations() {
        Station station = stationRepository.findById(2L).orElseThrow();
        station.setActive(false);
        stationRepository.saveAndFlush(station);

        List<Station> active = stationRepository.findByActiveTrueOrderByIdAsc();

        assertThat(active).extracting(Station::getId).containsExactly(1L, 3L);
    }

    @Test
    void findByExternalSourceAndExternalId_resolvesSeed() {
        Optional<Station> result = stationRepository
                .findByExternalSourceAndExternalId("TOMTOM", "tt-krk-1");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("GreenWay Galeria Kazimierz");
    }

    @Test
    void findLatestForStations_returnsLatestSnapshotPerStation() {
        Station station = stationRepository.findById(1L).orElseThrow();

        StationStatusSnapshot older = new StationStatusSnapshot();
        older.setStation(station);
        older.setSource("TOMTOM");
        older.setAvailableCount(0);
        older.setOccupiedCount(0);
        older.setReservedCount(0);
        older.setOutOfServiceCount(0);
        older.setUnknownCount(0);
        older.setRecordedAt(Instant.now().minusSeconds(3600));
        snapshotRepository.saveAndFlush(older);

        StationStatusSnapshot newest = new StationStatusSnapshot();
        newest.setStation(station);
        newest.setSource("TOMTOM");
        newest.setAvailableCount(7);
        newest.setOccupiedCount(1);
        newest.setReservedCount(0);
        newest.setOutOfServiceCount(0);
        newest.setUnknownCount(0);
        newest.setRecordedAt(Instant.now().plusSeconds(60));
        snapshotRepository.saveAndFlush(newest);

        List<StationStatusSnapshot> latest = snapshotRepository.findLatestForStations(List.of(1L, 2L, 3L));

        assertThat(latest)
                .filteredOn(s -> s.getStation().getId().equals(1L))
                .singleElement()
                .satisfies(s -> {
                    assertThat(s.getAvailableCount()).isEqualTo(7);
                    assertThat(s.getOccupiedCount()).isEqualTo(1);
                });

        assertThat(latest).extracting(s -> s.getStation().getId())
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void findTopByStationIdOrderByRecordedAtDesc_returnsMostRecentSnapshot() {
        Optional<StationStatusSnapshot> latest = snapshotRepository
                .findTopByStationIdOrderByRecordedAtDesc(2L);

        assertThat(latest).isPresent();
        assertThat(latest.get().getAvailableCount()).isEqualTo(1);
        assertThat(latest.get().getOccupiedCount()).isEqualTo(2);
    }
}
