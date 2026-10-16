package com.wildfiresync.core;

import java.util.Objects;
import java.util.UUID;

public final class Subjects {

	public static final String PREFIX = "events.operation";

	public static final String ALL = PREFIX + ".>";

	public static final String REPORT_PREFIX = "report";

	public static final String REPORTS_ALL = REPORT_PREFIX + ".>";

	private Subjects() {
	}

	public static String forRecord(RecordRef record) {
		Objects.requireNonNull(record, "record");
		return PREFIX + "." + record.type().name().toLowerCase() + "." + record.id();
	}

	public static String forReport(String hexagon, UUID reportId) {
		Objects.requireNonNull(reportId, "reportId");
		return REPORT_PREFIX + "." + checkHexagon(hexagon) + "." + reportId;
	}

	public static String reportsIn(String hexagon) {
		return REPORT_PREFIX + "." + checkHexagon(hexagon) + ".*";
	}

	private static String checkHexagon(String hexagon) {
		Objects.requireNonNull(hexagon, "hexagon");
		if (hexagon.isEmpty() || !isValidToken(hexagon)) {
			throw new IllegalArgumentException("not a valid subject token: '" + hexagon + "'");
		}
		return hexagon;
	}

	private static boolean isValidToken(String token) {
		for (int i = 0; i < token.length(); i++) {
			char c = token.charAt(i);
			if (c == '.' || c == '*' || c == '>' || Character.isWhitespace(c)) {
				return false;
			}
		}
		return true;
	}

}
