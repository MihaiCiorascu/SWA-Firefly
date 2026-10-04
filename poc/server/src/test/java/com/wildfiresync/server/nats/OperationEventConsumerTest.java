package com.wildfiresync.server.nats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.wildfiresync.core.SetSeverity;
import com.wildfiresync.server.service.MergeService;

import io.nats.client.Message;
import io.nats.client.StreamContext;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import tools.jackson.databind.ObjectMapper;

class OperationEventConsumerTest {

	private final MergeService mergeService = mock(MergeService.class);

	private final OperationEventConsumer consumer =
			new OperationEventConsumer(mock(StreamContext.class), new ObjectMapper(), mergeService);

	@Test
	void passesTheOperationToTheServiceAndAcknowledges() {
		Message message = messageWith(event("tablet-a", 1, 4));

		consumer.onMessage(message);

		ArgumentCaptor<SetSeverity> captor = ArgumentCaptor.forClass(SetSeverity.class);
		verify(mergeService).apply(captor.capture());
		SetSeverity operation = captor.getValue();
		assertEquals("tablet-a", operation.id().node().value());
		assertEquals(1, operation.id().seq());
		assertEquals("report-1", operation.report().value());
		assertEquals(4, operation.severity());
		assertEquals(Instant.parse("2026-09-27T10:15:00Z"), operation.recordedAt());
		verify(message).ack();
	}

	@Test
	void terminatesAMessageThatIsNotValidJson() {
		Message message = messageWith("not json");

		consumer.onMessage(message);

		verify(message).term();
		verify(message, never()).ack();
		verifyNoInteractions(mergeService);
	}

	@Test
	void terminatesAnOperationTheCoreRejects() {
		Message message = messageWith(event("tablet-a", 1, 99));

		consumer.onMessage(message);

		verify(message).term();
		verify(message, never()).ack();
		verifyNoInteractions(mergeService);
	}

	@Test
	void terminatesAnOperationWithAMissingField() {
		Message message = messageWith("""
				{"nodeId":"tablet-a","seq":1,"reportId":"report-1","severity":4}""");

		consumer.onMessage(message);

		verify(message).term();
		verifyNoInteractions(mergeService);
	}

	@Test
	void negativelyAcknowledgesWhenTheServiceFails() {
		doThrow(new IllegalStateException("database down")).when(mergeService).apply(any());
		Message message = messageWith(event("tablet-a", 1, 4));

		consumer.onMessage(message);

		verify(message).nak();
		verify(message, never()).ack();
		verify(message, never()).term();
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
