-- community_status_overrides: odpytywane przy każdym raporcie i co 60s przez frontend
CREATE INDEX community_overrides_station_state_idx
    ON community_status_overrides (station_id, state);

CREATE INDEX community_overrides_state_idx
    ON community_status_overrides (state);

-- station_reports: używane do sprawdzenia duplikatu głosu użytkownika
CREATE INDEX station_reports_lookup_idx
    ON station_reports (station_id, reporter_id, override_id);
