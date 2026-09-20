package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CoreTests {

	@Test
	void nameIsSet() {
		assertEquals("wildfire-sync-core", Core.NAME);
	}

}
