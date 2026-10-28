package com.wildfiresync.device

import com.wildfiresync.core.Zone
import com.wildfiresync.core.schema.FieldValues.Location
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerSyncTest {

    private val here = Location(52.12, 5.84)

    @Test
    fun `a tablet holding no reports listens around where it stands`() {
        assertEquals(Zone.around(here), zonesFor(here, emptyList()))
        assertEquals(9, zonesFor(here, emptyList()).size)
    }

    @Test
    fun `a report far away adds the zones around it`() {
        val far = Location(51.50, 4.20)

        assertEquals(Zone.around(here) + Zone.around(far), zonesFor(here, listOf(far)))
        assertEquals(18, zonesFor(here, listOf(far)).size)
    }

    @Test
    fun `a report where the tablet stands adds no zones`() {
        assertEquals(Zone.around(here), zonesFor(here, listOf(here)))
    }

    @Test
    fun `the mocked position is a valid place on the map`() {
        assertEquals(9, Zone.around(DevicePosition.current()).size)
    }

}
