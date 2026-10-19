package com.wildfiresync.core;

import com.wildfiresync.core.schema.FieldValues;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

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

    /** Every value written to the field, for IMMUTABLE, LARGEST_EVENT_ID and MANUAL policies */
    record Candidates(Set<Candidate> values) implements FieldState {

        public Candidates {
            Objects.requireNonNull(values, "values");
            values = Collections.unmodifiableSet(new LinkedHashSet<>(values));
        }

        public static Candidates of(Candidate candidate) {
            return new Candidates(Set.of(candidate));
        }

        public Candidates join(Candidates other) {
            Set<Candidate> union = new LinkedHashSet<>(values);
            union.addAll(other.values);
            return new Candidates(union);
        }

        public List<Candidate> ordered() {
            return values.stream().sorted().toList();
        }

    }

}
