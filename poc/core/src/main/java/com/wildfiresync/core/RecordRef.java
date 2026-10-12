package com.wildfiresync.core;

import com.wildfiresync.core.schema.Event.RecordType;
import java.util.Objects;
import java.util.UUID;

/** Identity of a report. */
public record RecordRef(RecordType type, UUID id) implements Comparable<RecordRef> {

	public RecordRef {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(id, "id");
	}

	public static RecordRef incident(UUID id) {
		return new RecordRef(RecordType.INCIDENT, id);
	}

	public static RecordRef report(UUID id) {
		return new RecordRef(RecordType.REPORT, id);
	}

	@Override
	public int compareTo(RecordRef other) {
		int byType = type.compareTo(other.type);
		return byType != 0 ? byType : id.compareTo(other.id);
	}

	@Override
	public String toString() {
		return type + "/" + id;
	}

}
