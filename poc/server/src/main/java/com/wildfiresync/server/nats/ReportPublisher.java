package com.wildfiresync.server.nats;

import java.io.IOException;
import java.util.UUID;

import com.wildfiresync.core.Subjects;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReportPublisher {

	private static final Logger log = LoggerFactory.getLogger(ReportPublisher.class);

	private final JetStream jetStream;

	public ReportPublisher(Connection connection) throws IOException {
		this.jetStream = connection.jetStream();
	}

	// TODO: called from MergeService once it holds merged state; hexagon derived from the merged location
	public void publish(String hexagon, UUID reportId, byte[] payload) throws IOException, JetStreamApiException {
		String subject = Subjects.forReport(hexagon, reportId);
		jetStream.publish(subject, payload);
		log.info("published report {} on {}", reportId, subject);
	}

}
