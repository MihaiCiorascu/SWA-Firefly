package com.wildfiresync.server.service;

import com.wildfiresync.core.SetSeverity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MergeService {

	private static final Logger log = LoggerFactory.getLogger(MergeService.class);

	// TODO: merge the operation into the stored state of its report
	public void apply(SetSeverity operation) {
		log.info("{} received for report {} severity {}", operation.id(), operation.report(), operation.severity());
	}

}
