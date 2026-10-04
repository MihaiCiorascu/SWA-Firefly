package com.wildfiresync.core;

import java.util.Objects;

public final class Subjects {

	public static final String PREFIX = "events.operation";

	public static final String ALL = PREFIX + ".>";

	private Subjects() {
	}

	public static String forReport(ReportId report) {
		Objects.requireNonNull(report, "report");
		String token = report.value();
		if (!isValidToken(token)) {
			throw new IllegalArgumentException("report id is not usable as a subject token: " + token);
		}
		return PREFIX + "." + token;
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
