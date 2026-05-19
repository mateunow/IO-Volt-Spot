CREATE TABLE connector_reports (
    id              BIGSERIAL PRIMARY KEY,
    station_id      BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    connector_id    BIGINT NOT NULL REFERENCES station_connectors(id) ON DELETE CASCADE,
    reporter_id     BIGINT NOT NULL REFERENCES users(id),
    reported_status VARCHAR(20) NOT NULL
        CHECK (reported_status IN ('WORKING', 'NOT_WORKING', 'OCCUPIED')),
    occupied_count  INT,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX connector_reports_user_connector_uq
    ON connector_reports (connector_id, reporter_id);

CREATE INDEX connector_reports_station_expires_idx
    ON connector_reports (station_id, expires_at);
