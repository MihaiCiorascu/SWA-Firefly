package com.wildfiresync.core;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.wildfiresync.core.schema.CausalContext;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The JSON in the two text columns of the events table, CausalContext and supersedes.
 */
public final class StorageJson {

	private StorageJson() {
	}

	/** A causal context as a JSON object of node id to counter, keys sorted. */
	public static String writeContext(CausalContext context) {
		Objects.requireNonNull(context, "context");
		Map<String, Long> sorted = new TreeMap<>();
		context.counters().forEach((node, counter) -> sorted.put(node.value(), counter));
		StringWriter out = new StringWriter();
		try (JsonWriter writer = new JsonWriter(out)) {
			writer.beginObject();
			for (Map.Entry<String, Long> entry : sorted.entrySet()) {
				writer.name(entry.getKey()).value(entry.getValue());
			}
			writer.endObject();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return out.toString();
	}

	public static CausalContext readContext(String json) {
		Objects.requireNonNull(json, "json");
		Map<NodeId, Long> counters = new LinkedHashMap<>();
		try (JsonReader reader = new JsonReader(new StringReader(json))) {
			reader.setStrictness(Strictness.STRICT);
			reader.beginObject();
			while (reader.hasNext()) {
				String node = reader.nextName();
				if (reader.peek() != JsonToken.NUMBER) {
					throw new IllegalArgumentException("the counter of " + node + " must be a number, got " + reader.peek());
				}
				if (counters.put(new NodeId(node), reader.nextLong()) != null) {
					throw new IllegalArgumentException(node + " is given twice");
				}
			}
			reader.endObject();
			requireEnd(reader);
		}
		catch (IOException | IllegalStateException | NumberFormatException ex) {
			throw new IllegalArgumentException("a causal context must be a JSON object of node to counter: "
					+ ex.getMessage(), ex);
		}
		return new CausalContext(counters);
	}

	/** Event ids as a JSON array of strings, sorted. */
	public static String writeIds(Set<UUID> ids) {
		Objects.requireNonNull(ids, "ids");
		List<String> sorted = new ArrayList<>(ids.size());
		ids.forEach(id -> sorted.add(id.toString()));
		sorted.sort(null);
		StringWriter out = new StringWriter();
		try (JsonWriter writer = new JsonWriter(out)) {
			writer.beginArray();
			for (String id : sorted) {
				writer.value(id);
			}
			writer.endArray();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return out.toString();
	}

	public static Set<UUID> readIds(String json) {
		Objects.requireNonNull(json, "json");
		Set<UUID> ids = new LinkedHashSet<>();
		try (JsonReader reader = new JsonReader(new StringReader(json))) {
			reader.setStrictness(Strictness.STRICT);
			reader.beginArray();
			while (reader.hasNext()) {
				if (reader.peek() != JsonToken.STRING) {
					throw new IllegalArgumentException("an event id must be a JSON string, got " + reader.peek());
				}
				ids.add(UUID.fromString(reader.nextString()));
			}
			reader.endArray();
			requireEnd(reader);
		}
		catch (IOException | IllegalStateException ex) {
			throw new IllegalArgumentException("supersedes must be a JSON array of event ids: " + ex.getMessage(), ex);
		}
		return ids;
	}

	private static void requireEnd(JsonReader reader) throws IOException {
		if (reader.peek() != JsonToken.END_DOCUMENT) {
			throw new IllegalArgumentException("unexpected content after the value");
		}
	}

}
