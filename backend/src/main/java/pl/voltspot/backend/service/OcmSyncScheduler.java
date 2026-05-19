package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OcmSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(OcmSyncScheduler.class);

    private static final double MIN_LAT = 49.0;
    private static final double MAX_LAT = 54.9;
    private static final double MIN_LON = 14.1;
    private static final double MAX_LON = 24.2;

    private final StationService stationService;
    private final StationReportService stationReportService;
    private final ConnectorReportService connectorReportService;

    @Scheduled(cron = "0 0 */12 * * *")
    public void syncPoland() {
        log.info("Starting scheduled OCM sync for Poland...");
        try {
            stationService.fetchStationsFromOCM(MIN_LAT, MIN_LON, MAX_LAT, MAX_LON);
            log.info("Scheduled OCM sync for Poland completed.");
        } catch (Exception ex) {
            log.error("Scheduled OCM sync for Poland failed.", ex);
        }
        stationReportService.expireOverrides();
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void expireConnectorReports() {
        connectorReportService.expireReports();
    }
}
