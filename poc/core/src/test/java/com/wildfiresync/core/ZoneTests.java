package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wildfiresync.core.schema.FieldValues.Location;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ZoneTests {

	private static final UUID REPORT = UUID.fromString("00000000-0000-4000-8000-00000000000a");

	@Test
	void thePlaceDecidesTheToken() {
		assertEquals(Zone.of(new Location(52.12, 5.84)), Zone.of(52.12, 5.84));
		assertEquals("z2842x3716", Zone.of(52.12, 5.84));
	}

	@Test
	void everyTokenIsALegalSubjectToken() {
		for (double latitude : new double[] { -90, -52.12, 0, 52.12, 90 }) {
			for (double longitude : new double[] { -180, -5.84, 0, 5.84, 180 }) {
				String zone = Zone.of(latitude, longitude);
				assertEquals("report." + zone + "." + REPORT, Subjects.forReport(zone, REPORT));
			}
		}
	}

	@Test
	void twoCrewsInTheSameCellPublishToTheSameZone() {
		assertEquals(Zone.of(52.121, 5.841), Zone.of(52.123, 5.843));
	}

	@Test
	void aPlaceACountyAwayIsADifferentZone() {
		assertNotEquals(Zone.of(52.12, 5.84), Zone.of(51.90, 5.84));
		assertNotEquals(Zone.of(52.12, 5.84), Zone.of(52.12, 6.20));
	}

	@Test
	void aTabletSubscribesToItsOwnCellAndTheRingAroundIt() {
		Set<String> around = Zone.around(new Location(52.12, 5.84));

		assertEquals(9, around.size());
		assertTrue(around.contains(Zone.of(52.12, 5.84)), around.toString());
		assertTrue(around.contains(Zone.of(52.12 + Zone.CELL_DEGREES, 5.84)));
		assertTrue(around.contains(Zone.of(52.12 - Zone.CELL_DEGREES, 5.84)));
		assertTrue(around.contains(Zone.of(52.12, 5.84 + Zone.CELL_DEGREES)));
		assertTrue(around.contains(Zone.of(52.12, 5.84 - Zone.CELL_DEGREES)));
	}

	@Test
	void theRingWrapsRoundTheAntimeridian() {
		Set<String> around = Zone.around(0, 179.99);

		assertEquals(9, around.size());
		assertTrue(around.contains(Zone.of(0, -179.99)), around.toString());
	}

	@Test
	void theRingStopsAtThePole() {
		assertEquals(6, Zone.around(90, 0).size());
		assertEquals(6, Zone.around(-90, 0).size());
	}

	@Test
	void bothMeridiansAreTheSamePlace() {
		assertEquals(Zone.of(0, -180), Zone.of(0, 180));
	}

	@ParameterizedTest
	@ValueSource(doubles = { 90.0001, -90.0001, 1000 })
	void aLatitudeOffTheMapIsRejected(double latitude) {
		assertThrows(IllegalArgumentException.class, () -> Zone.of(latitude, 0));
	}

	@ParameterizedTest
	@ValueSource(doubles = { 180.0001, -180.0001, 1000 })
	void aLongitudeOffTheMapIsRejected(double longitude) {
		assertThrows(IllegalArgumentException.class, () -> Zone.of(0, longitude));
	}

}
