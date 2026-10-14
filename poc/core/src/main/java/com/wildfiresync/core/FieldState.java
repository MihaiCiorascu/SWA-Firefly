package com.wildfiresync.core;

import com.wildfiresync.core.schema.FieldValues;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * The merged state of one field.
 */
public sealed interface FieldState permits FieldState.MaxValue, FieldState.UnionValue, FieldState.Candidates {

    /** Highest value wins; equal values are credited to the lower event id so both devices agree. */
    record MaxValue(int value, java.util.UUID eventId) implements FieldState {

        public MaxValue {
            Objects.requireNonNull(eventId, "eventId");
        }

        public MaxValue join(MaxValue other) {
            if (value != other.value) {
                return value > other.value ? this : other;
            }
            return eventId.compareTo(other.eventId) <= 0 ? this : other;
        }

    }

    /** Grow-only set of labels */
    record UnionValue(Set<FieldValues.ReportType> values) implements FieldState {

        public UnionValue {
            Objects.requireNonNull(values, "values");
            values = Collections.unmodifiableSet(new TreeSet<>(values));
        }

        public UnionValue join(UnionValue other) {
            Set<FieldValues.ReportType> union = new TreeSet<>(values);
            union.addAll(other.values);
            return new UnionValue(union);
        }

    }

    /**
     * Every value written to the field, for IMMUTABLE, LARGEST_EVENT_ID and MANUAL policies, plus the event ids that a
     * RESOLVE_CONFLICT settled. Both only grow, so the join stays a plain union, and which values are still current is
     * worked out from them when reading.
     */
    record Candidates(Set<Candidate> values, Set<UUID> superseded) implements FieldState {

        public Candidates {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(superseded, "superseded");
            values = Collections.unmodifiableSet(new LinkedHashSet<>(values));
            superseded = Collections.unmodifiableSet(new TreeSet<>(superseded));
        }

        public static Candidates of(Candidate candidate) {
            return new Candidates(Set.of(candidate), Set.of());
        }

        public Candidates join(Candidates other) {
            Set<Candidate> union = new LinkedHashSet<>(values);
            union.addAll(other.values);
            Set<UUID> settled = new TreeSet<>(superseded);
            settled.addAll(other.superseded);
            return new Candidates(union, settled);
        }

        /** The current values: not settled by a resolution, and not seen by the writer of another value. */
        public List<Candidate> ordered() {
            return values.stream()
                    .filter(candidate -> !superseded.contains(candidate.eventId()))
                    .filter(candidate -> values.stream().noneMatch(other -> other.replaces(candidate)))
                    .sorted()
                    .toList();
        }

        public List<Candidate> all() {
            return values.stream().sorted().toList();
        }

    }

}
