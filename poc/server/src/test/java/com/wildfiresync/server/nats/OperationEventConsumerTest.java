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
import java.util.UUID;

import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.server.service.MergeService;

import io.nats.client.Message;
import io.nats.client.StreamContext;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import tools.jackson.databind.ObjectMapper;

class OperationEventConsumerTest {

	private static final String EVENT_ID = "00000000-0000-0000-0000-0000000000aa";

	private static final String REPORT_ID = "00000000-0000-0000-0000-000000000001";

	private static final String INCIDENT_ID = "00000000-0000-0000-0000-000000000009";

	private final MergeService mergeService = mock(MergeService.class);

	private final OperationEventConsumer consumer =
			new OperationEventConsumer(mock(StreamContext.class), new ObjectMapper(), mergeService);

	@Test
	void passesTheEventToTheServiceAndAcknowledges() {
		Message message = messageWith(severityEvent("4"));

		consumer.onMessage(message);

		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
		verify(mergeService).apply(captor.capture());
		Event event = captor.getValue();
		assertEquals(UUID.fromString(EVENT_ID), event.eventId());
		assertEquals(UUID.fromString(REPORT_ID), event.recordId());
		assertEquals(Event.RecordType.REPORT, event.recordType());
		assertEquals(Event.Operation.SET_FIELD, event.operation());
		assertEquals(Field.REPORT_SEVERITY.key(), event.field());
		assertEquals("4", event.value());
		assertEquals("tablet-a", event.dot().node().value());
		assertEquals(1, event.dot().seq());
		assertEquals(Instant.parse("2026-09-27T10:15:00Z"), event.createdAt());
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
	void terminatesAValueTheSchemaRejects() {
		Message message = messageWith(severityEvent("99"));

		consumer.onMessage(message);

		verify(message).term();
		verify(message, never()).ack();
		verifyNoInteractions(mergeService);
	}

	@Test
	void passesACreateWithItsIncidentId() {
		Message message = messageWith(createEvent("{\\\"incidentId\\\":\\\"%s\\\"}".formatted(INCIDENT_ID)));

		consumer.onMessage(message);

		verify(mergeService).apply(any());
		verify(message).ack();
	}

	@Test
	void terminatesACreateWithoutItsIncidentId() {
		Message message = messageWith(createEvent("{}"));

		consumer.onMessage(message);

		verify(message).term();
		verifyNoInteractions(mergeService);
	}

	@Test
	void terminatesAnEventThatChangesTheIncidentIdAfterTheCreate() {
		Message message = messageWith(severityEvent(INCIDENT_ID).replace("\"severity\"", "\"incidentId\""));

		consumer.onMessage(message);

		verify(message).term();
		verifyNoInteractions(mergeService);
	}

	@Test
	void terminatesAnEventWithAMissingField() {
		Message message = messageWith("""
				{"eventId":"%s","recordId":"%s","recordType":"REPORT","operation":"SET_FIELD",\
				"field":"severity","value":"4","actorId":"crew-tablet-a","nodeId":"tablet-a","seq":1}"""
				.formatted(EVENT_ID, REPORT_ID));

		consumer.onMessage(message);

		verify(message).term();
		verifyNoInteractions(mergeService);
	}

	@Test
	void negativelyAcknowledgesWhenTheServiceFails() {
		doThrow(new IllegalStateException("database down")).when(mergeService).apply(any());
		Message message = messageWith(severityEvent("4"));

		consumer.onMessage(message);

		verify(message).nak();
		verify(message, never()).ack();
		verify(message, never()).term();
	}

	private static String severityEvent(String severity) {
		return """
				{"eventId":"%s","recordId":"%s","recordType":"REPORT","operation":"SET_FIELD",\
				"field":"severity","value":"%s","actorId":"crew-tablet-a",\
				"createdAt":"2026-09-27T10:15:00Z","nodeId":"tablet-a","seq":1,"context":{},"supersedes":[]}"""
				.formatted(EVENT_ID, REPORT_ID, severity);
	}

	private static String createEvent(String value) {
		return """
				{"eventId":"%s","recordId":"%s","recordType":"REPORT","operation":"CREATE",\
				"value":"%s","actorId":"crew-tablet-a",\
				"createdAt":"2026-09-27T10:14:00Z","nodeId":"tablet-a","seq":1,"context":{},"supersedes":[]}"""
				.formatted(EVENT_ID, REPORT_ID, value);
	}

	private static Message messageWith(String payload) {
		Message message = mock(Message.class);
		when(message.getData()).thenReturn(payload.getBytes(StandardCharsets.UTF_8));
		return message;
	}

}
