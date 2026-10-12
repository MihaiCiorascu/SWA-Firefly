package com.wildfiresync.core.schema;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.wildfiresync.core.schema.Event.RecordType;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CreateValues {

	private CreateValues() {
	}

	public static String format(RecordType recordType, Map<Field, FieldValue> values) {
		Objects.requireNonNull(recordType, "recordType");
		Objects.requireNonNull(values, "values");
		requireFields(recordType, values);
		List<Map.Entry<Field, FieldValue>> entries = new ArrayList<>(values.entrySet());
		entries.sort(Comparator.comparing(entry -> entry.getKey().key()));
		StringWriter out = new StringWriter();
		try (JsonWriter writer = new JsonWriter(out)) {
			writer.beginObject();
			for (Map.Entry<Field, FieldValue> entry : entries) {
				Field field = entry.getKey();
				String stored = FieldValues.format(entry.getValue());
				if (!FieldValues.parse(field, stored).equals(entry.getValue())) {
					throw new IllegalArgumentException(field + " cannot hold " + entry.getValue());
				}
				writer.name(field.key()).value(stored);
			}
			writer.endObject();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return out.toString();
	}

	public static Map<Field, FieldValue> parse(RecordType recordType, String json) {
		Objects.requireNonNull(recordType, "recordType");
		Objects.requireNonNull(json, "json");
		Map<Field, FieldValue> values = new EnumMap<>(Field.class);
		try (JsonReader reader = new JsonReader(new StringReader(json))) {
			reader.setStrictness(Strictness.STRICT);
			reader.beginObject();
			while (reader.hasNext()) {
				Field field = Field.of(recordType, reader.nextName());
				if (reader.peek() != JsonToken.STRING) {
					throw new IllegalArgumentException(field + " must be a JSON string, got " + reader.peek());
				}
				if (values.put(field, FieldValues.parse(field, reader.nextString())) != null) {
					throw new IllegalArgumentException(field + " is given twice");
				}
			}
			reader.endObject();
			if (reader.peek() != JsonToken.END_DOCUMENT) {
				throw new IllegalArgumentException("unexpected content after the object");
			}
		}
		catch (IOException | IllegalStateException ex) {
			throw new IllegalArgumentException("a CREATE value must be a JSON object of field values: " + ex.getMessage(),
					ex);
		}
		requireFields(recordType, values);
		return Collections.unmodifiableMap(values);
	}

	private static void requireFields(RecordType recordType, Map<Field, FieldValue> values) {
		for (Field field : values.keySet()) {
			if (field.recordType() != recordType) {
				throw new IllegalArgumentException(field + " is not a field of " + recordType);
			}
		}
		for (Field field : Field.values()) {
			if (field.recordType() == recordType && field.policy() == Field.Policy.IMMUTABLE
					&& !values.containsKey(field)) {
				throw new IllegalArgumentException("a CREATE of a " + recordType + " must set " + field.key());
			}
		}
	}

}
