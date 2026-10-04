package com.wildfiresync.core.schema;

import com.wildfiresync.core.OpId;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record Event(
		UUID eventId,
		UUID recordId,
		RecordType recordType,
		Operation operation,
		String field,
		String value,
		String actorId,
		Instant createdAt,
		OpId dot,
		CausalContext causalContext,
		Set<UUID> supersedes
) {

	public enum RecordType {
		INCIDENT,
		REPORT
	}

	public enum Operation {
		CREATE,
		SET_FIELD,
		RESOLVE_CONFLICT
	}

	public Event {
		Objects.requireNonNull(eventId, "eventId");
		Objects.requireNonNull(recordId, "recordId");
		Objects.requireNonNull(recordType, "recordType");
		Objects.requireNonNull(operation, "operation");
		Objects.requireNonNull(actorId, "actorId");
		Objects.requireNonNull(createdAt, "createdAt");
		Objects.requireNonNull(dot, "dot");
		causalContext = causalContext == null ? CausalContext.EMPTY : causalContext;
		supersedes = supersedes == null ? Set.of() : Set.copyOf(supersedes);
		if (operation == Operation.CREATE) {
			if (field != null || value != null) {
				throw new IllegalArgumentException("a CREATE event carries no field or value");
			}
		}
		else {
			Objects.requireNonNull(field, "field");
			Objects.requireNonNull(value, "value");
		}
	}
}
