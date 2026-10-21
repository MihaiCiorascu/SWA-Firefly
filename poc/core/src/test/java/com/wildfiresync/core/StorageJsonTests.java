package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wildfiresync.core.schema.CausalContext;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StorageJsonTests {

	private static final UUID ONE = UUID.fromString("11111111-1111-4111-8111-111111111111");

	private static final UUID TWO = UUID.fromString("22222222-2222-4222-8222-222222222222");

	@Test
	void aContextIsWrittenWithItsNodesSorted() {
		CausalContext context = new CausalContext(Map.of(new NodeId("tablet-b"), 1L, new NodeId("tablet-a"), 2L));

		assertEquals("{\"tablet-a\":2,\"tablet-b\":1}", StorageJson.writeContext(context));
	}

	@Test
	void aContextSurvivesTheRoundTrip() {
		CausalContext context = new CausalContext(Map.of(new NodeId("tablet-a"), 7L, new NodeId("tablet-b"), 3L));

		assertEquals(context, StorageJson.readContext(StorageJson.writeContext(context)));
	}

	@Test
	void anEmptyContextIsAnEmptyObject() {
		assertEquals("{}", StorageJson.writeContext(CausalContext.EMPTY));
		assertEquals(CausalContext.EMPTY, StorageJson.readContext("{}"));
	}

	@Test
	void aNodeIdNeedingEscapingStillRoundTrips() {
		// nothing stops a tablet id holding a quote or a backslash, and the column is text
		CausalContext context = new CausalContext(Map.of(new NodeId("crew \"a\"\\b"), 4L));

		assertEquals(context, StorageJson.readContext(StorageJson.writeContext(context)));
	}

	@Test
	void idsAreWrittenSorted() {
		assertEquals("[\"" + ONE + "\",\"" + TWO + "\"]", StorageJson.writeIds(Set.of(TWO, ONE)));
	}

	@Test
	void idsSurviveTheRoundTrip() {
		assertEquals(Set.of(ONE, TWO), StorageJson.readIds(StorageJson.writeIds(Set.of(TWO, ONE))));
		assertEquals("[]", StorageJson.writeIds(Set.of()));
		assertEquals(Set.of(), StorageJson.readIds("[]"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "{", "{\"tablet-a\":\"2\"}", "{\"tablet-a\":2} trailing", "[1]", "null" })
	void rubbishIsNotAContext(String json) {
		assertThrows(IllegalArgumentException.class, () -> StorageJson.readContext(json));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "[", "[1]", "[\"not a uuid\"]", "[] trailing", "{}" })
	void rubbishIsNotAnIdList(String json) {
		assertThrows(IllegalArgumentException.class, () -> StorageJson.readIds(json));
	}

}
