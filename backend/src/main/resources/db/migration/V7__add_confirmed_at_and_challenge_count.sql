ALTER TABLE community_status_overrides ADD COLUMN confirmed_at TIMESTAMPTZ NULL;
ALTER TABLE community_status_overrides ADD COLUMN challenge_count INT NOT NULL DEFAULT 0;
