package com.wildfiresync.core;

import com.wildfiresync.core.schema.FieldValue;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public sealed interface Resolution permits Resolution.Settled, Resolution.Conflicted {
    record Settled(FieldValue value, Optional<UUID> source) implements Resolution {

        public Settled {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(source, "source");
        }

    }

    /** Wrapper for the list of candidate records, for consideration by a crew leader. */
    record Conflicted(List<Candidate> candidates) implements Resolution {

        public Conflicted {
            Objects.requireNonNull(candidates, "candidates");
            candidates = List.copyOf(candidates);
        }

    }

}
