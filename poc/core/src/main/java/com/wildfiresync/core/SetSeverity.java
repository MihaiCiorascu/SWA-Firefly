package com.wildfiresync.core;

import java.time.Instant;
import java.util.Objects;

/**
 * A node authors a report containing a severity. In the future, the min max should be decided by the schema,
 * but for this release they are hardcoded here. This file will be changed eventually to handle all strictly increasing
 * field types.
 */
public record SetSeverity(OpId id, ReportId report, int severity, Instant recordedAt) {

	public static final int MIN = 1;

	public static final int MAX = 10;

	public SetSeverity {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(report, "report");
		Objects.requireNonNull(recordedAt, "recordedAt");
		if (severity < MIN || severity > MAX) {
			throw new IllegalArgumentException(
					"severity must be between " + MIN + " and " + MAX + ", got " + severity);
		}
	}

}
