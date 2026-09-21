package com.wildfiresync.core;

import java.util.Objects;

/**
 * Unique identity of an operation: the authoring node and that node's sequence number (for use with DVVs).
 */
public record OpId(NodeId node, long seq) implements Comparable<OpId> {

	public OpId {
		Objects.requireNonNull(node, "node");
		if (seq < 1) {
			throw new IllegalArgumentException("seq starts at 1, got " + seq);
		}
	}

	@Override
	public int compareTo(OpId other) {
		int byNode = node.compareTo(other.node);
		return byNode != 0 ? byNode : Long.compare(seq, other.seq);
	}

	@Override
	public String toString() {
		return node + ":" + seq;
	}

}
