package com.wildfiresync.core.schema;

import com.wildfiresync.core.OpId;
import java.time.Instant;
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

}
