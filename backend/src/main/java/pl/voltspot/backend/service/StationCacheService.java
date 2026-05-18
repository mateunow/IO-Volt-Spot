package pl.voltspot.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.voltspot.backend.dto.station.StationMarkerResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationCacheService {

    private static final Logger log = LoggerFactory.getLogger(StationCacheService.class);

    private final StationService stationService;

    private volatile List<StationMarkerResponse> cached = List.of();
    private volatile String etag = "\"empty\"";

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedDelay = 120_000)
    public void scheduledRefresh() {
        refresh();
    }

    public void refresh() {
        try {
            List<StationMarkerResponse> fresh = stationService.getStations(null, null, null, null);
            cached = fresh;
            etag = '"' + Integer.toHexString(fresh.hashCode()) + '"';
            log.debug("Station cache refreshed: {} stations, etag={}", fresh.size(), etag);
        } catch (Exception e) {
            log.error("Station cache refresh failed", e);
        }
    }

    public List<StationMarkerResponse> getCached() {
        return cached;
    }

    public String getEtag() {
        return etag;
    }
}
