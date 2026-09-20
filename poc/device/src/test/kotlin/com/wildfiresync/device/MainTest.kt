package com.wildfiresync.device

import com.wildfiresync.core.Core
import kotlin.test.Test
import kotlin.test.assertEquals

class MainTest {

    @Test
    fun coreIsAccessible() {
        assertEquals("wildfire-sync-core", Core.NAME)
    }
}
