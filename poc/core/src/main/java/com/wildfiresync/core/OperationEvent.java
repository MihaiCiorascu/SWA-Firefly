package com.wildfiresync.core;

import java.time.Instant;
import java.util.Objects;

public record OperationEvent(String nodeId, long seq, String reportId, int severity, Instant recordedAt) {

	public OperationEvent {
		Objects.requireNonNull(nodeId, "nodeId");
		Objects.requireNonNull(reportId, "reportId");
		Objects.requireNonNull(recordedAt, "recordedAt");
	}

	public static OperationEvent of(SetSeverity operation) {
		return new OperationEvent(operation.id().node().value(), operation.id().seq(), operation.report().value(),
				operation.severity(), operation.recordedAt());
	}

	public SetSeverity toOperation() {
		return new SetSeverity(new OpId(new NodeId(nodeId), seq), new ReportId(reportId), severity, recordedAt);
	}

}
