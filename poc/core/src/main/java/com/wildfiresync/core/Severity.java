package com.wildfiresync.core;

import java.util.Objects;

/**
 * The merged severity of one report, and the operation it is credited to.
 */
public record Severity(int value, OpId source) {

	public Severity {
		Objects.requireNonNull(source, "source");
	}

	/**
	 * Commutative, associative and idempotent. Severity can only increase, as decided in the report.
	 */
	public Severity join(Severity other) {
		if (value != other.value) {
			return value > other.value ? this : other;
		}
		return source.compareTo(other.source) <= 0 ? this : other;
	}

}
