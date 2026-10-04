package com.wildfiresync.core.schema;

import com.wildfiresync.core.NodeId;
import com.wildfiresync.core.OpId;
import java.util.Map;

public record CausalContext(Map<NodeId, Long> counters) {

	public static final CausalContext EMPTY = new CausalContext(Map.of());

	public CausalContext {
		counters = Map.copyOf(counters);
	}

	public boolean covers(OpId dot) {
		return counters.getOrDefault(dot.node(), 0L) >= dot.seq();
	}

}
