package com.wildfiresync.server.nats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import com.wildfiresync.core.ReportId;

import io.nats.client.Message;
import io.nats.client.StreamContext;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class OperationEventConsumerTest {

	private static final ReportId REPORT = new ReportId("report-1");

	private final OperationEventConsumer consumer =
			new OperationEventConsumer(mock(StreamContext.class), new ObjectMapper());

	@Test
	void mergesAnOperationAndAcknowledgesIt() {
		Message message = messageWith("""
				{"nodeId":"tablet-a","seq":1,"reportId":"report-1","severity":4,\
				"recordedAt":"2026-09-27T10:15:00Z"}""");

		consumer.onMessage(message);

		assertEquals(4, consumer.world().severityOf(REPORT).orElseThrow().value());
		verify(message).ack();
		verify(message, never()).term();
	}

	@Test
	void mergesConcurrentOperationsRegardlessOfOrder() {
		consumer.onMessage(messageWith(event("tablet-a", 1, 3)));
		consumer.onMessage(messageWith(event("tablet-b", 1, 6)));
		consumer.onMessage(messageWith(event("tablet-c", 1, 2)));

		assertEquals(6, consumer.world().severityOf(REPORT).orElseThrow().value());
	}

	@Test
	void redeliveryOfTheSameOperationChangesNothing() {
		consumer.onMessage(messageWith(event("tablet-a", 1, 5)));
		var afterFirst = consumer.world();

		consumer.onMessage(messageWith(event("tablet-a", 1, 5)));

		assertEquals(afterFirst, consumer.world());
	}

	@Test
	void terminatesAMessageThatIsNotValidJson() {
		Message message = messageWith("not json");

		consumer.onMessage(message);

		verify(message).term();
		verify(message, never()).ack();
		assertTrue(consumer.world().reports().isEmpty());
	}

	@Test
	void terminatesAnOperationTheCoreRejects() {
		Message message = messageWith(event("tablet-a", 1, 99));

		consumer.onMessage(message);

		verify(message).term();
		verify(message, never()).ack();
		assertTrue(consumer.world().reports().isEmpty());
	}

	private static String event(String node, long seq, int severity) {
		return """
				{"nodeId":"%s","seq":%d,"reportId":"report-1","severity":%d,\
				"recordedAt":"2026-09-27T10:15:00Z"}""".formatted(node, seq, severity);
	}

	private static Message messageWith(String payload) {
		Message message = mock(Message.class);
		when(message.getData()).thenReturn(payload.getBytes(StandardCharsets.UTF_8));
		return message;
	}

}
