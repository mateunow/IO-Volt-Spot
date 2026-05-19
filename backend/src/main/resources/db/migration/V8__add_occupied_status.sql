ALTER TABLE community_status_overrides DROP CONSTRAINT community_status_overrides_reported_status_check;
ALTER TABLE community_status_overrides ADD CONSTRAINT community_status_overrides_reported_status_check CHECK (reported_status IN ('WORKING', 'NOT_WORKING', 'OCCUPIED'));

ALTER TABLE station_reports DROP CONSTRAINT station_reports_reported_status_check;
ALTER TABLE station_reports ADD CONSTRAINT station_reports_reported_status_check CHECK (reported_status IN ('WORKING', 'NOT_WORKING', 'OCCUPIED'));
