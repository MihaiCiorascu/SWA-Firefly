package com.wildfiresync.server.nats;

import java.io.IOException;

import com.wildfiresync.core.Subjects;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.StreamContext;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NatsProperties.class)
public class NatsConfig {

	static final String STREAM = "operation-events";

	private static final Logger log = LoggerFactory.getLogger(NatsConfig.class);

	@Bean(destroyMethod = "close")
	public Connection natsConnection(NatsProperties properties) throws IOException, InterruptedException {
		return Nats.connect(properties.url());
	}

	@Bean
	public StreamContext operationEventsStream(Connection connection) throws IOException, JetStreamApiException {
		StreamConfiguration configuration = StreamConfiguration.builder()
				.name(STREAM)
				.subjects(Subjects.ALL)
				.storageType(StorageType.File)
				.build();

		JetStreamManagement management = connection.jetStreamManagement();
		try {
			management.addStream(configuration);
		}
		catch (JetStreamApiException ex) {
			management.updateStream(configuration);
		}

		log.info("stream {} ready for {}", STREAM, Subjects.ALL);
		return connection.getStreamContext(STREAM);
	}

}
