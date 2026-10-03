package com.wildfiresync.server.nats;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wildfiresync.nats")
public record NatsProperties(String url) {
}
