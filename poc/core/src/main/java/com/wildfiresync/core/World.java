package com.wildfiresync.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Everything a node knows: the merged severity of every report it has heard about. */
public record World(Map<RecordRef, RecordState> records) {

	public static final World EMPTY = new World(Map.of());

	public World {
		Objects.requireNonNull(records, "records");
		records = Collections.unmodifiableMap(new TreeMap<>(records));
	}

	public Optional<RecordState> record(RecordRef ref) {
		return Optional.ofNullable(records.get(ref));
	}

	/** Report-wise join. A report known to only one side is carried over unchanged. */
	public World join(World other) {
		Map<RecordRef, RecordState> merged = new TreeMap<>(records);
		other.records.forEach((ref, incoming) -> merged.merge(ref, incoming, RecordState::join));
		return new World(merged);
	}

}
