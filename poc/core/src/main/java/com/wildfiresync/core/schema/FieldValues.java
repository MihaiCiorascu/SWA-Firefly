package com.wildfiresync.core.schema;

public final class FieldValues {

	private FieldValues() {
	}

	public enum IncidentStatus {
		ACTIVE,
		CONTAINED,
		CLOSED
	}

	public enum ReportType {
		SMOKE,
		FIRE,
		ROAD_BLOCKED
	}

	public record Location(double latitude, double longitude) {
	}

}
