package com.wildfiresync.core;

import java.util.Objects;

/** Identity of a report. */
public record ReportId(String value) implements Comparable<ReportId> {

	public ReportId {
		Objects.requireNonNull(value, "value");
		if (value.isBlank()) {
			throw new IllegalArgumentException("report id must not be blank");
		}
	}

	@Override
	public int compareTo(ReportId other) {
		return value.compareTo(other.value);
	}

	@Override
	public String toString() {
		return value;
	}

}
