package com.wildfiresync.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Everything a node knows: the merged severity of every report it has heard about. */
public record World(Map<ReportId, Severity> reports) {

	public static final World EMPTY = new World(Map.of());

	public World {
		Objects.requireNonNull(reports, "reports");
		reports = Collections.unmodifiableMap(new TreeMap<>(reports));
	}

	public Optional<Severity> severityOf(ReportId report) {
		return Optional.ofNullable(reports.get(report));
	}

	/** Report-wise join. A report known to only one side is carried over unchanged. */
	public World join(World other) {
		Map<ReportId, Severity> merged = new TreeMap<>(reports);
		other.reports.forEach((id, incoming) -> merged.merge(id, incoming, Severity::join));
		return new World(merged);
	}

}
