package com.wildfiresync.device

import kotlin.test.Test
import kotlin.test.assertEquals

class ReportConsumerTest {

    @Test
    fun `one filter subject per hexagon`() {
        assertEquals(
            setOf("report.8a1fb46622dffff.*", "report.8a1fb46622d7fff.*"),
            filterSubjects(setOf("8a1fb46622dffff", "8a1fb46622d7fff")).toSet(),
        )
    }

    @Test
    fun `an empty hexagon set gives no filters`() {
        assertEquals(emptyList(), filterSubjects(emptySet()))
    }

}
