package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.voltspot.backend.entity.StationFeedback;

import java.util.List;

public interface StationFeedbackRepository extends JpaRepository<StationFeedback, Long> {
    List<StationFeedback> findByStationIdOrderByCreatedAtDesc(Long stationId);
}