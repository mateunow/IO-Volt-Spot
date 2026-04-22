CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       display_name VARCHAR(100) NOT NULL,
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT chk_users_role CHECK (role IN ('USER', 'OWNER', 'ADMIN'))
);

CREATE TABLE stations (
                          id BIGSERIAL PRIMARY KEY,
                          external_source VARCHAR(30) NOT NULL,
                          external_id VARCHAR(120) NOT NULL,
                          name VARCHAR(255) NOT NULL,
                          latitude DOUBLE PRECISION NOT NULL,
                          longitude DOUBLE PRECISION NOT NULL,
                          address_line VARCHAR(255),
                          city VARCHAR(120),
                          country VARCHAR(120),
                          operator_name VARCHAR(255),
                          opening_hours TEXT,
                          access_type VARCHAR(120),
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          last_synced_at TIMESTAMPTZ,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT uq_station_source_external UNIQUE (external_source, external_id)
);

CREATE TABLE station_connectors (
                                    id BIGSERIAL PRIMARY KEY,
                                    station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
                                    connector_type VARCHAR(100) NOT NULL,
                                    current_type VARCHAR(20),
                                    power_kw NUMERIC(8,2),
                                    quantity INTEGER NOT NULL DEFAULT 1,
                                    external_connector_key VARCHAR(120),
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE station_status_snapshots (
                                          id BIGSERIAL PRIMARY KEY,
                                          station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
                                          source VARCHAR(30) NOT NULL,
                                          available_count INTEGER NOT NULL DEFAULT 0,
                                          occupied_count INTEGER NOT NULL DEFAULT 0,
                                          reserved_count INTEGER NOT NULL DEFAULT 0,
                                          out_of_service_count INTEGER NOT NULL DEFAULT 0,
                                          unknown_count INTEGER NOT NULL DEFAULT 0,
                                          recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE station_feedback (
                                  id BIGSERIAL PRIMARY KEY,
                                  station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
                                  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                  operational_status VARCHAR(20) NOT NULL,
                                  comment TEXT,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT chk_station_feedback_status CHECK (operational_status IN ('WORKING', 'NOT_WORKING', 'BUSY', 'LIMITED', 'UNKNOWN'))
);

CREATE TABLE station_owners (
                                id BIGSERIAL PRIMARY KEY,
                                station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
                                user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT uq_station_owner UNIQUE (station_id, user_id)
);

CREATE INDEX idx_stations_location ON stations(latitude, longitude);
CREATE INDEX idx_station_connectors_station_id ON station_connectors(station_id);
CREATE INDEX idx_station_status_snapshots_station_id_recorded_at
    ON station_status_snapshots(station_id, recorded_at DESC);
CREATE INDEX idx_station_feedback_station_id ON station_feedback(station_id);
CREATE INDEX idx_station_feedback_user_id ON station_feedback(user_id);
CREATE INDEX idx_station_owners_station_id ON station_owners(station_id);
CREATE INDEX idx_station_owners_user_id ON station_owners(user_id);