package com.wildfiresync.server.service;

import com.wildfiresync.core.schema.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MergeService {

	private static final Logger log = LoggerFactory.getLogger(MergeService.class);

	// TODO: merge the event into the stored state of its record
	public void apply(Event event) {
		log.info("{} {} received for {} {}: {} = {}", event.dot(), event.operation(), event.recordType(),
				event.recordId(), event.field(), event.value());
	}

}
