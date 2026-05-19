CREATE TABLE community_status_overrides (
    id                    BIGSERIAL PRIMARY KEY,
    station_id            BIGINT      NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    reported_status       VARCHAR(20) NOT NULL CHECK (reported_status IN ('WORKING', 'NOT_WORKING')),
    state                 VARCHAR(20) NOT NULL CHECK (state IN ('PENDING', 'CONFIRMED', 'REJECTED', 'EXPIRED')),
    working_count         INT         NOT NULL DEFAULT 0,
    not_working_count     INT         NOT NULL DEFAULT 0,
    created_at            TIMESTAMP   NOT NULL DEFAULT NOW(),
    expires_at            TIMESTAMP,
    confirmed_by_admin_id BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE station_reports (
    id               BIGSERIAL PRIMARY KEY,
    station_id       BIGINT      NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    reporter_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_status  VARCHAR(20) NOT NULL CHECK (reported_status IN ('WORKING', 'NOT_WORKING')),
    override_id      BIGINT REFERENCES community_status_overrides(id) ON DELETE SET NULL,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);
