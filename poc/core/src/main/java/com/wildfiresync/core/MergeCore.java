package com.wildfiresync.core;

import java.util.Map;

/**
 * The merging library main algorithm. Merges operations into current states known by every device.
 */
public final class MergeCore {

	private MergeCore() {
	}

	/**
	 * Merge one operation, as a join between the existing state and a new world containing just the op.
	 */
	public static World apply(World world, SetSeverity op) {
		return world.join(lift(op));
	}

	/** Creates a world containing just one operation for the purpose of merging. */
	public static World lift(SetSeverity op) {
		return new World(Map.of(op.report(), new Severity(op.severity(), op.id())));
	}

	/** Applies a batch of operations. */
	public static World applyAll(World world, Iterable<SetSeverity> ops) {
		World result = world;
		for (SetSeverity op : ops) {
			result = apply(result, op);
		}
		return result;
	}

}
