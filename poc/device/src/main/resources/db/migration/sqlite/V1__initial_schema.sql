-- Device schema (SQLite). Must stay logically identical to the server's
-- db/migration/postgres/V1__initial_schema.sql.
-- Foreign keys are off by default in SQLite: run PRAGMA foreign_keys = ON on every connection.

-- Source of truth. Every replica stores the same rows; everything else is derived from them.
-- UUIDs are lowercase canonical text so string order matches the eventId tiebreak.
CREATE TABLE events (
    event_id       TEXT    NOT NULL PRIMARY KEY CHECK (length(event_id) = 36 AND event_id = lower(event_id)),
    record_id      TEXT    NOT NULL CHECK (length(record_id) = 36 AND record_id = lower(record_id)),
    record_type    TEXT    NOT NULL CHECK (record_type IN ('INCIDENT', 'REPORT')),
    operation      TEXT    NOT NULL CHECK (operation IN ('CREATE', 'SET_FIELD', 'RESOLVE_CONFLICT')),
    field          TEXT,
    value          TEXT    NOT NULL,
    actor_id       TEXT    NOT NULL,
    created_at     TEXT    NOT NULL,
    dot_device_id  TEXT    NOT NULL,
    dot_sequence   INTEGER NOT NULL CHECK (dot_sequence >= 1),
    causal_context TEXT    NOT NULL,
    supersedes     TEXT,
    CONSTRAINT events_dot_unique UNIQUE (dot_device_id, dot_sequence),
    CONSTRAINT events_field_matches_operation CHECK ((operation = 'CREATE') = (field IS NULL)),
    CONSTRAINT events_supersedes_matches_operation CHECK ((operation = 'RESOLVE_CONFLICT') = (supersedes IS NOT NULL))
) STRICT;

CREATE INDEX events_record_field_idx ON events (record_id, field);

-- This replica's identity and DVV state. One row, written at enrolment.
CREATE TABLE replica_state (
    id             INTEGER NOT NULL PRIMARY KEY CHECK (id = 1),
    node_id        TEXT    NOT NULL,
    next_sequence  INTEGER NOT NULL CHECK (next_sequence >= 1),
    causal_context TEXT    NOT NULL
) STRICT;

-- Projections, rebuildable from events. A column is NULL while its field is unknown
-- (events not received yet) or in conflict (candidates are listed in field_conflicts).
CREATE TABLE incidents (
    incident_id TEXT NOT NULL PRIMARY KEY,
    name        TEXT,
    status      TEXT CHECK (status IN ('ACTIVE', 'CONTAINED', 'CLOSED'))
) STRICT;

CREATE TABLE reports (
    report_id    TEXT    NOT NULL PRIMARY KEY,
    incident_id  TEXT,
    report_types TEXT,
    latitude     REAL    CHECK (latitude BETWEEN -90 AND 90),
    longitude    REAL    CHECK (longitude BETWEEN -180 AND 180),
    severity     INTEGER CHECK (severity BETWEEN 1 AND 10),
    description  TEXT,
    CONSTRAINT reports_location_complete CHECK ((latitude IS NULL) = (longitude IS NULL))
) STRICT;

CREATE INDEX reports_incident_idx ON reports (incident_id);

-- Candidate events for every Tier B field that currently has concurrent values.
CREATE TABLE field_conflicts (
    record_id TEXT NOT NULL,
    field     TEXT NOT NULL,
    event_id  TEXT NOT NULL REFERENCES events (event_id),
    PRIMARY KEY (record_id, field, event_id)
) STRICT;
