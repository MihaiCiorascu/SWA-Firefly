package com.wildfiresync.core;

import com.wildfiresync.core.schema.FieldValues.Location;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The zone a report belongs to, as the middle token of its subject.
 *
 * Fixed grid for the PoC. Switching over to H3 requires modifying only this file.
 */
public final class Zone {

	/** The side of one cell in degrees. */
	public static final double CELL_DEGREES = 0.05;

	private static final int ROWS = (int) Math.round(180 / CELL_DEGREES);

	private static final int COLUMNS = (int) Math.round(360 / CELL_DEGREES);

	private Zone() {
	}

	/** The token of the cell holding this location. */
	public static String of(Location location) {
		Objects.requireNonNull(location, "location");
		return of(location.latitude(), location.longitude());
	}

	public static String of(double latitude, double longitude) {
		return token(row(latitude), column(longitude));
	}

	/**
	 * This location's cell and the eight around it.
	 */
	public static Set<String> around(Location location) {
		Objects.requireNonNull(location, "location");
		return around(location.latitude(), location.longitude());
	}

	public static Set<String> around(double latitude, double longitude) {
		int row = row(latitude);
		int column = column(longitude);
		Set<String> tokens = new LinkedHashSet<>();
		for (int dr = -1; dr <= 1; dr++) {
			int r = row + dr;
			if (r < 0 || r >= ROWS) {
				continue;
			}
			for (int dc = -1; dc <= 1; dc++) {
				tokens.add(token(r, Math.floorMod(column + dc, COLUMNS)));
			}
		}
		return tokens;
	}

	private static String token(int row, int column) {
		return "z" + row + "x" + column;
	}

	private static int row(double latitude) {
		if (latitude < -90 || latitude > 90 || Double.isNaN(latitude)) {
			throw new IllegalArgumentException("latitude is outside the map: " + latitude);
		}
		// 90 degrees exactly would land one past the last row, so it shares the pole cell below it
		return Math.min((int) Math.floor((latitude + 90) / CELL_DEGREES), ROWS - 1);
	}

	private static int column(double longitude) {
		if (longitude < -180 || longitude > 180 || Double.isNaN(longitude)) {
			throw new IllegalArgumentException("longitude is outside the map: " + longitude);
		}
		// the two meridians are the same place, so 180 wraps onto the column of -180
		return Math.floorMod((int) Math.floor((longitude + 180) / CELL_DEGREES), COLUMNS);
	}

}
