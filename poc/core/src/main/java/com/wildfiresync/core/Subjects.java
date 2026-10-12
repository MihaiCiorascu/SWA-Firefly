package com.wildfiresync.core;

import java.util.Objects;

public final class Subjects {

	public static final String PREFIX = "events.operation";

	public static final String ALL = PREFIX + ".>";

	private Subjects() {
	}

	public static String forRecord(RecordRef record) {
		Objects.requireNonNull(record, "record");
		return PREFIX + "." + record.type().name().toLowerCase() + "." + record.id();
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
