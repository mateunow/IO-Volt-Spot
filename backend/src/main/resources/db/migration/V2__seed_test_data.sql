INSERT INTO users (id, email, password_hash, display_name, role, active)
VALUES
    (1, 'owner@voltspot.pl', '$2a$10$7yK2g9dQ3T5wG4F8g0fL2e3VhP0J4Q5e6T7Y8U9I0oP1aS2dF3g4K', 'Owner Demo', 'OWNER', TRUE),
    (2, 'user@voltspot.pl', '$2a$10$7yK2g9dQ3T5wG4F8g0fL2e3VhP0J4Q5e6T7Y8U9I0oP1aS2dF3g4K', 'User Demo', 'USER', TRUE),
    (3, 'admin@voltspot.pl', '$2a$10$7yK2g9dQ3T5wG4F8g0fL2e3VhP0J4Q5e6T7Y8U9I0oP1aS2dF3g4K', 'Admin Demo', 'ADMIN', TRUE);

INSERT INTO stations (
    id, external_source, external_id, name, latitude, longitude, address_line, city, country,
    operator_name, opening_hours, access_type, is_active, last_synced_at
)
VALUES
    (1, 'TOMTOM', 'tt-krk-1', 'GreenWay Galeria Kazimierz', 50.049683, 19.960582,
     'ul. Podgórska 34', 'Kraków', 'Polska', 'GreenWay', '24/7', 'PUBLIC', TRUE, CURRENT_TIMESTAMP),

    (2, 'TOMTOM', 'tt-krk-2', 'Orlen EV Kraków Mogilska', 50.070140, 19.971180,
     'ul. Mogilska 86', 'Kraków', 'Polska', 'Orlen Charge', '24/7', 'PUBLIC', TRUE, CURRENT_TIMESTAMP),

    (3, 'OPEN_CHARGE_MAP', 'ocm-krk-1', 'TAURON Ładowarka Bonarka', 50.022200, 19.926000,
     'ul. Kamieńskiego 11', 'Kraków', 'Polska', 'TAURON', '08:00-22:00', 'PUBLIC', TRUE, CURRENT_TIMESTAMP);

INSERT INTO station_connectors (id, station_id, connector_type, current_type, power_kw, quantity, external_connector_key)
VALUES
    (1, 1, 'CCS2', 'DC', 50.00, 2, 'ccs2-50'),
    (2, 1, 'Type2', 'AC', 22.00, 2, 'type2-22'),
    (3, 2, 'CCS2', 'DC', 100.00, 2, 'ccs2-100'),
    (4, 2, 'CHAdeMO', 'DC', 50.00, 1, 'chademo-50'),
    (5, 3, 'Type2', 'AC', 22.00, 2, 'type2-22');

INSERT INTO station_status_snapshots (
    id, station_id, source, available_count, occupied_count, reserved_count, out_of_service_count, unknown_count, recorded_at
)
VALUES
    (1, 1, 'TOMTOM', 2, 1, 0, 0, 1, CURRENT_TIMESTAMP - INTERVAL '5 minutes'),
    (2, 2, 'TOMTOM', 1, 2, 0, 0, 0, CURRENT_TIMESTAMP - INTERVAL '3 minutes'),
    (3, 3, 'OPEN_CHARGE_MAP', 1, 0, 0, 1, 0, CURRENT_TIMESTAMP - INTERVAL '10 minutes');

INSERT INTO station_feedback (id, station_id, user_id, operational_status, comment, created_at, updated_at)
VALUES
    (1, 1, 2, 'WORKING', 'Działa bez problemu, ładowanie rozpoczęło się od razu.', CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    (2, 2, 2, 'BUSY', 'Były zajęte wszystkie miejsca.', CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
    (3, 3, 1, 'LIMITED', 'Jedno stanowisko nie działało.', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours');

INSERT INTO station_owners (id, station_id, user_id, assigned_at)
VALUES
    (1, 1, 1, CURRENT_TIMESTAMP - INTERVAL '7 days'),
    (2, 3, 1, CURRENT_TIMESTAMP - INTERVAL '5 days');

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('stations', 'id'), (SELECT MAX(id) FROM stations));
SELECT setval(pg_get_serial_sequence('station_connectors', 'id'), (SELECT MAX(id) FROM station_connectors));
SELECT setval(pg_get_serial_sequence('station_status_snapshots', 'id'), (SELECT MAX(id) FROM station_status_snapshots));
SELECT setval(pg_get_serial_sequence('station_feedback', 'id'), (SELECT MAX(id) FROM station_feedback));
SELECT setval(pg_get_serial_sequence('station_owners', 'id'), (SELECT MAX(id) FROM station_owners));