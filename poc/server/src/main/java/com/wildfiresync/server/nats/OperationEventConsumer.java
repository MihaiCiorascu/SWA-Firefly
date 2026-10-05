package com.wildfiresync.server.nats;

import java.io.IOException;

import com.wildfiresync.core.OperationEvent;
import com.wildfiresync.core.SetSeverity;
import com.wildfiresync.core.Subjects;
import com.wildfiresync.server.service.MergeService;

import io.nats.client.ConsumerContext;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.StreamContext;
import io.nats.client.api.ConsumerConfiguration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class OperationEventConsumer {

	static final String DURABLE = "server-merge";

	private static final Logger log = LoggerFactory.getLogger(OperationEventConsumer.class);

	private final StreamContext stream;

	private final ObjectMapper objectMapper;

	private final MergeService mergeService;

	private MessageConsumer consumer;

	public OperationEventConsumer(StreamContext stream, ObjectMapper objectMapper, MergeService mergeService) {
		this.stream = stream;
		this.objectMapper = objectMapper;
		this.mergeService = mergeService;
	}

	@PostConstruct
	public void start() throws IOException, JetStreamApiException {
		ConsumerContext context = stream.createOrUpdateConsumer(ConsumerConfiguration.builder()
				.durable(DURABLE)
				.filterSubject(Subjects.ALL)
				.build());
		consumer = context.consume(this::onMessage);
		log.info("consuming {} as {}", Subjects.ALL, DURABLE);
	}

	@PreDestroy
	public void stop() {
		if (consumer != null) {
			consumer.stop();
		}
	}

	void onMessage(Message message) {
		SetSeverity operation;
		try {
			operation = objectMapper.readValue(message.getData(), OperationEvent.class).toOperation();
		}
		catch (RuntimeException ex) {
			log.error("discarding unusable message on {}: {}", message.getSubject(), ex.toString());
			message.term();
			return;
		}

		try {
			mergeService.apply(operation);
			message.ack();
		}
		catch (RuntimeException ex) {
			log.error("could not apply {}, will retry: {}", operation.id(), ex.toString());
			message.nak();
		}
	}

}
