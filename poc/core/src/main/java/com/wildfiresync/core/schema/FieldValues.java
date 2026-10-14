package com.wildfiresync.core.schema;

import java.util.*;
import java.util.stream.Collectors;

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


	public static FieldValue parse(Field field, String stored) {
		Objects.requireNonNull(field, "field");
		Objects.requireNonNull(stored, "stored");
		try {
			return switch (field.kind()) {
				case NUMBER -> number(field, stored);
				case TEXT -> new FieldValue.Text(stored);
				case STATUS -> new FieldValue.Status(IncidentStatus.valueOf(stored));
				case REPORT_TYPES -> reportTypes(stored);
				case LOCATION -> location(stored);
				case REFERENCE -> new FieldValue.Reference(UUID.fromString(stored));
			};
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException(field + " cannot hold " + quote(stored) + ": " + ex.getMessage(), ex);
		}
	}

	public static String format(FieldValue value) {
		return switch (value) {
			case FieldValue.Num num -> Integer.toString(num.value());
			case FieldValue.Text text -> text.value();
			case FieldValue.Status status -> status.value().name();
			case FieldValue.Types types ->
					types.values().stream().map(Enum::name).collect(Collectors.joining(","));
			case FieldValue.Place place -> place.value().latitude() + "," + place.value().longitude();
			case FieldValue.Reference reference -> reference.value().toString();
		};
	}

	private static FieldValue number(Field field, String stored) {
		int parsed = Integer.parseInt(stored.trim());
		field.range().ifPresent(range -> {
			if (parsed < range.min() || parsed > range.max()) {
				throw new IllegalArgumentException(
						"must be between " + range.min() + " and " + range.max() + ", got " + parsed);
			}
		});
		return new FieldValue.Num(parsed);
	}

	private static FieldValue reportTypes(String stored) {
		Set<ReportType> types = new TreeSet<>();
		for (String token : stored.split(",")) {
			String trimmed = token.trim();
			if (!trimmed.isEmpty()) {
				types.add(ReportType.valueOf(trimmed));
			}
		}
		if (types.isEmpty()) {
			throw new IllegalArgumentException("no report types given");
		}
		return new FieldValue.Types(types);
	}

	private static FieldValue location(String stored) {
		String[] parts = stored.split(",");
		if (parts.length != 2) {
			throw new IllegalArgumentException("expected latitude,longitude");
		}
		double latitude = Double.parseDouble(parts[0].trim());
		double longitude = Double.parseDouble(parts[1].trim());
		// NaN fails both checks too
		if (!(latitude >= -90 && latitude <= 90) || !(longitude >= -180 && longitude <= 180)) {
			throw new IllegalArgumentException("latitude must be within [-90, 90] and longitude within [-180, 180]");
		}
		return new FieldValue.Place(new Location(latitude, longitude));
	}

	private static String quote(String stored) {
		return "\"" + stored + "\"";
	}
}
