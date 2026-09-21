package com.wildfiresync.core;

import java.util.Objects;

/**
 * Identity of a node in the system. Must be unique, assigned during enrolment.
 */
public record NodeId(String value) implements Comparable<NodeId> {

	public NodeId {
		Objects.requireNonNull(value, "value");
		if (value.isBlank()) {
			throw new IllegalArgumentException("node id must not be blank");
		}
	}

	@Override
	public int compareTo(NodeId other) {
		return value.compareTo(other.value);
	}

	@Override
	public String toString() {
		return value;
	}

}
