package com.wildfiresync.server.nats;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.wildfiresync.core.EventBundle;
import com.wildfiresync.core.Subjects;
import com.wildfiresync.core.schema.Event;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/**
 * Publishes a merged report on the subject of its zone, which is what a tablet entering that zone
 * subscribes to.
 *
 * <p>The payload is every event of the record rather than its merged row. A tablet cannot merge a row
 * into its own event log, but it can merge events, so this way a tablet that has been away ends up
 * agreeing with the server instead of showing two answers side by side. Because each message carries
 * the whole record, the reports stream only needs to keep the last one per subject.
 */
@Component
public class ReportPublisher {

	private static final Logger log = LoggerFactory.getLogger(ReportPublisher.class);

	private final JetStream jetStream;

	private final ObjectMapper objectMapper;

	public ReportPublisher(Connection connection, ObjectMapper objectMapper) throws IOException {
		this.jetStream = connection.jetStream();
		this.objectMapper = objectMapper;
	}

	public void publish(String zone, EventBundle bundle) {
		send(zone, bundle);
		log.info("published report {} with {} event(s) to zone {}", bundle.recordId(), bundle.events().size(), zone);
	}

	/**
	 * Replaces what a zone holds for a report with an empty bundle, for a report whose settled location
	 * moved it somewhere else. Without this, a tablet arriving in the old zone would be handed the
	 * report as though it were still there.
	 */
	public void withdraw(String zone, UUID report) {
		send(zone, new EventBundle(report, Event.RecordType.REPORT, List.of()));
		log.info("withdrew report {} from zone {}", report, zone);
	}

	private void send(String zone, EventBundle bundle) {
		String subject = Subjects.forReport(zone, bundle.recordId());
		try {
			jetStream.publish(subject, objectMapper.writeValueAsBytes(bundle));
		}
		catch (IOException | JetStreamApiException ex) {
			// the event is stored either way, so the consumer naks and the publish is tried again
			throw new IllegalStateException("could not publish to " + subject + ": " + ex.getMessage(), ex);
		}
	}

}
