package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OperationEventTest {

	private static final Instant RECORDED_AT = Instant.parse("2026-09-27T10:15:00Z");

	@Test
	void convertsToTheCoreOperation() {
		OperationEvent event = new OperationEvent("tablet-a", 7, "report-1", 4, RECORDED_AT);

		SetSeverity operation = event.toOperation();

		assertEquals("tablet-a", operation.id().node().value());
		assertEquals(7, operation.id().seq());
		assertEquals("report-1", operation.report().value());
		assertEquals(4, operation.severity());
		assertEquals(RECORDED_AT, operation.recordedAt());
	}

	@Test
	void roundTripsThroughTheWireForm() {
		SetSeverity operation =
				new SetSeverity(new OpId(new NodeId("tablet-a"), 7), new ReportId("report-1"), 4, RECORDED_AT);

		assertEquals(operation, OperationEvent.of(operation).toOperation());
	}

	@Test
	void rejectsSeverityOutsideTheAllowedRange() {
		OperationEvent event = new OperationEvent("tablet-a", 1, "report-1", 99, RECORDED_AT);

		assertThrows(IllegalArgumentException.class, event::toOperation);
	}

	@Test
	void rejectsBlankNodeId() {
		OperationEvent event = new OperationEvent("", 1, "report-1", 4, RECORDED_AT);

		assertThrows(IllegalArgumentException.class, event::toOperation);
	}

}
