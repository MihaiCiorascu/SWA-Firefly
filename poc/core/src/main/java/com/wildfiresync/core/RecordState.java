package com.wildfiresync.core;

import com.wildfiresync.core.schema.Field;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One incident or report: whether it exists yet, and the merged state of each of its fields.
 */
public record RecordState(boolean created, Map<Field, FieldState> fields) {

    public static final RecordState EMPTY = new RecordState(false, Map.of());

    public RecordState {
        Objects.requireNonNull(fields, "fields");
        Map<Field, FieldState> copy = new EnumMap<>(Field.class);
        copy.putAll(fields);
        fields = Collections.unmodifiableMap(copy);
    }

    public Optional<FieldState> field(Field field) {
        return Optional.ofNullable(fields.get(field));
    }

    public RecordState join(RecordState other) {
        Map<Field, FieldState> merged = new EnumMap<>(Field.class);
        merged.putAll(fields);
        other.fields.forEach((field, incoming) -> merged.merge(field, incoming,
                (a, b) -> MergeCore.joinFields(field, a, b)));
        return new RecordState(created || other.created, merged);
    }

}
