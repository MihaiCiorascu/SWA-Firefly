package com.wildfiresync.core;

import com.wildfiresync.core.schema.FieldValue;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One value a device wrote to a field, with enough provenance for a crew leader to judge it.
 */
public record Candidate(FieldValue value, UUID eventId, OpId dot, String actorId, Instant createdAt)
        implements Comparable<Candidate> {

    public Candidate {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(dot, "dot");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Ordered by event id */
    @Override
    public int compareTo(Candidate other) {
        return eventId.compareTo(other.eventId);
    }

}