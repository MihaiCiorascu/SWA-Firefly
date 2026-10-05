package com.wildfiresync.core.schema;

import com.wildfiresync.core.schema.Event.RecordType;
import java.util.Arrays;

public enum Field {

	INCIDENT_NAME(RecordType.INCIDENT, "name", Policy.LARGEST_EVENT_ID),
	INCIDENT_STATUS(RecordType.INCIDENT, "status", Policy.MANUAL),
	REPORT_INCIDENT_ID(RecordType.REPORT, "incidentId", Policy.IMMUTABLE),
	REPORT_TYPE(RecordType.REPORT, "reportType", Policy.UNION),
	REPORT_LOCATION(RecordType.REPORT, "location", Policy.MANUAL),
	REPORT_SEVERITY(RecordType.REPORT, "severity", Policy.MAX),
	REPORT_DESCRIPTION(RecordType.REPORT, "description", Policy.MANUAL);

	public enum Tier {
		IMMUTABLE,
		A,
		B
	}

	public enum Policy {
		IMMUTABLE,
		LARGEST_EVENT_ID,
		UNION,
		MAX,
		MANUAL
	}

	private final RecordType recordType;

	private final String key;

	private final Policy policy;

	Field(RecordType recordType, String key, Policy policy) {
		this.recordType = recordType;
		this.key = key;
		this.policy = policy;
	}

	public static Field of(RecordType recordType, String key) {
		return Arrays.stream(values())
				.filter(field -> field.recordType == recordType && field.key.equals(key))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(recordType + " has no field " + key));
	}

	public RecordType recordType() {
		return recordType;
	}

	public String key() {
		return key;
	}

	public Policy policy() {
		return policy;
	}

	public Tier tier() {
		return switch (policy) {
			case IMMUTABLE -> Tier.IMMUTABLE;
			case MANUAL -> Tier.B;
			case LARGEST_EVENT_ID, UNION, MAX -> Tier.A;
		};
	}

}
