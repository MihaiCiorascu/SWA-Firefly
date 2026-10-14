package com.wildfiresync.core;

import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.FieldValue;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One value a device wrote to a field, with enough provenance for a crew leader to judge it. The context is what the
 * writer had seen, so a value it covers was replaced rather than written concurrently.
 */
public record Candidate(FieldValue value, UUID eventId, OpId dot, String actorId, Instant createdAt,
                        CausalContext context) implements Comparable<Candidate> {

    public Candidate {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(dot, "dot");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(context, "context");
    }

    public static Candidate of(FieldValue value, Event event) {
        return new Candidate(value, event.eventId(), event.dot(), event.actorId(), event.createdAt(),
                event.causalContext());
    }

    /** True when the writer of this value had seen the other one. */
    public boolean replaces(Candidate other) {
        return !equals(other) && context.covers(other.dot);
    }

    /** Ordered by event id */
    @Override
    public int compareTo(Candidate other) {
        return eventId.compareTo(other.eventId);
    }

}