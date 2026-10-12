package com.wildfiresync.core.schema;

import java.util.*;

public sealed interface FieldValue permits FieldValue.Num, FieldValue.Text, FieldValue.Status, FieldValue.Types,
        FieldValue.Place, FieldValue.Reference {

    record Num(int value) implements FieldValue {
    }

    record Text(String value) implements FieldValue {

        public Text {
            Objects.requireNonNull(value, "value");
        }

    }

    record Status(FieldValues.IncidentStatus value) implements FieldValue {

        public Status {
            Objects.requireNonNull(value, "value");
        }

    }

    record Types(Set<FieldValues.ReportType> values) implements FieldValue {

        public Types {
            Objects.requireNonNull(values, "values");
            values = Collections.unmodifiableSet(new TreeSet<>(values));
        }

        public static Types of(FieldValues.ReportType... values) {
            return new Types(Set.of(values));
        }

    }

    record Place(FieldValues.Location value) implements FieldValue {

        public Place {
            Objects.requireNonNull(value, "value");
        }

    }

    record Reference(UUID value) implements FieldValue {

        public Reference {
            Objects.requireNonNull(value, "value");
        }

    }

}
