-- Server schema (PostgreSQL). Must stay logically identical to the device's
-- db/migration/sqlite/V1__initial_schema.sql.
-- JSON columns are text, not jsonb: jsonb rewrites key order and whitespace, and every
-- replica must store the exact JSON the event was created with.

-- Source of truth. Every replica stores the same rows; everything else is derived from them.
CREATE TABLE events (
    event_id       uuid        NOT NULL PRIMARY KEY,
    record_id      uuid        NOT NULL,
    record_type    text        NOT NULL CHECK (record_type IN ('INCIDENT', 'REPORT')),
    operation      text        NOT NULL CHECK (operation IN ('CREATE', 'SET_FIELD', 'RESOLVE_CONFLICT')),
    field          text,
    value          text        NOT NULL,
    actor_id       text        NOT NULL,
    created_at     timestamptz NOT NULL,
    dot_device_id  text        NOT NULL,
    dot_sequence   bigint      NOT NULL CHECK (dot_sequence >= 1),
    causal_context text        NOT NULL,
    supersedes     text,
    CONSTRAINT events_dot_unique UNIQUE (dot_device_id, dot_sequence),
    CONSTRAINT events_field_matches_operation CHECK ((operation = 'CREATE') = (field IS NULL)),
    CONSTRAINT events_supersedes_matches_operation CHECK ((operation = 'RESOLVE_CONFLICT') = (supersedes IS NOT NULL))
);

CREATE INDEX events_record_field_idx ON events (record_id, field);

-- This replica's identity and DVV state. One row, written at enrolment.
CREATE TABLE replica_state (
    id             smallint NOT NULL PRIMARY KEY CHECK (id = 1),
    node_id        text     NOT NULL,
    next_sequence  bigint   NOT NULL CHECK (next_sequence >= 1),
    causal_context text     NOT NULL
);

-- Projections, rebuildable from events. A column is NULL while its field is unknown
-- (events not received yet) or in conflict (candidates are listed in field_conflicts).
CREATE TABLE incidents (
    incident_id uuid NOT NULL PRIMARY KEY,
    name        text,
    status      text CHECK (status IN ('ACTIVE', 'CONTAINED', 'CLOSED'))
);

CREATE TABLE reports (
    report_id    uuid             NOT NULL PRIMARY KEY,
    incident_id  uuid,
    report_types text,
    latitude     double precision CHECK (latitude BETWEEN -90 AND 90),
    longitude    double precision CHECK (longitude BETWEEN -180 AND 180),
    severity     integer          CHECK (severity BETWEEN 1 AND 10),
    description  text,
    CONSTRAINT reports_location_complete CHECK ((latitude IS NULL) = (longitude IS NULL))
);

CREATE INDEX reports_incident_idx ON reports (incident_id);

-- Candidate events for every Tier B field that currently has concurrent values.
CREATE TABLE field_conflicts (
    record_id uuid NOT NULL,
    field     text NOT NULL,
    event_id  uuid NOT NULL REFERENCES events (event_id),
    PRIMARY KEY (record_id, field, event_id)
);
