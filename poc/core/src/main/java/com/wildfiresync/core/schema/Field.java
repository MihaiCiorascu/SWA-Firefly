package com.wildfiresync.core.schema;

import com.wildfiresync.core.schema.Event.RecordType;
import java.util.Arrays;
import java.util.Optional;

public enum Field {

	INCIDENT_NAME(RecordType.INCIDENT, "name", Policy.LARGEST_EVENT_ID, Kind.TEXT),
	INCIDENT_STATUS(RecordType.INCIDENT, "status", Policy.MANUAL, Kind.STATUS),
	REPORT_INCIDENT_ID(RecordType.REPORT, "incidentId", Policy.IMMUTABLE, Kind.REFERENCE),
	REPORT_TYPE(RecordType.REPORT, "reportType", Policy.UNION, Kind.REPORT_TYPES),
	REPORT_LOCATION(RecordType.REPORT, "location", Policy.MANUAL, Kind.LOCATION),
	REPORT_SEVERITY(RecordType.REPORT, "severity", Policy.MAX, Kind.NUMBER, new Range(1, 10)),
	REPORT_DESCRIPTION(RecordType.REPORT, "description", Policy.MANUAL, Kind.TEXT);

	public enum Tier {
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

	public enum Kind {
		NUMBER,
		TEXT,
		STATUS,
		REPORT_TYPES,
		LOCATION,
		REFERENCE
	}

	private final RecordType recordType;

	private final String key;

	private final Policy policy;

	private final Kind kind;

	public record Range(int min, int max) {
	}
	private final Range range;

	Field(RecordType recordType, String key, Policy policy, Kind kind) {
		this(recordType, key, policy, kind, null);
	}

	Field(RecordType recordType, String key, Policy policy, Kind kind, Range range) {
		this.recordType = recordType;
		this.key = key;
		this.policy = policy;
		this.kind = kind;
		this.range = range;
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

	public Kind kind() {
		return kind;
	}

	public Optional<Range> range() {
		return Optional.ofNullable(range);
	}

	public Tier tier() {
		return policy == Policy.MANUAL ? Tier.B : Tier.A;
	}

}
