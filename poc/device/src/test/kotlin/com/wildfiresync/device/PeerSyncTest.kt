package com.wildfiresync.device

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.RecordRef
import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValue
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReplicaStateRepository
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PeerSyncTest {

    private val incident = UUID.fromString("2f1c6a43-0d5e-4c4b-9a3e-6c1f0b8e7d21")

    private val databases = mutableListOf<LocalDatabase>()

    @AfterTest
    fun close() {
        databases.forEach { it.close() }
    }

    /** tablets linked by name, a send hands the bytes straight to the other side */
    private inner class Network(private val scope: CoroutineScope) {
        val syncs = mutableMapOf<String, PeerSync>()
        private val links = mutableSetOf<Pair<String, String>>()

        fun tablet(name: String): TabletStore {
            val store = TabletStore {
                val database = LocalDatabase.inMemory(RuntimeEnvironment.getApplication()).also { databases += it }
                ReplicaStateRepository(database).enrol(NodeId(name))
                ReportService(database)
            }
            syncs[name] = PeerSync(store, transport(name), scope)
            return store
        }

        fun connect(a: String, b: String) {
            links += a to b
            links += b to a
            syncs.getValue(a).onConnected(b)
            syncs.getValue(b).onConnected(a)
        }

        fun disconnect(a: String, b: String) {
            links -= a to b
            links -= b to a
        }

        private fun transport(self: String) = object : PeerSync.Transport {
            override fun peers(): List<String> = links.filter { it.first == self }.map { it.second }

            override fun send(peer: String, bytes: ByteArray): Boolean {
                if (self to peer !in links) return false
                check(syncs.getValue(peer).handle(self, bytes))
                return true
            }
        }
    }

    /** waits until every message, and every answer to it, has been handled */
    private suspend fun CoroutineScope.settle() {
        while (true) {
            val running = coroutineContext[Job]!!.children.filter { it.isActive }.toList()
            if (running.isEmpty()) return
            running.joinAll()
        }
    }

    private suspend fun TabletStore.newReport(): UUID = write { createReport(incident) }.recordId()

    private suspend fun TabletStore.set(report: UUID, field: Field, value: FieldValue) =
        write { setField(RecordRef.report(report), field, value) }

    private suspend fun TabletStore.events() = read { allEvents() }.toSet()

    @Test
    fun `two tablets that connect end with the same record`() = runBlocking {
        val network = Network(this)
        val alpha = network.tablet("tablet-a")
        val bravo = network.tablet("tablet-b")
        val first = alpha.newReport()
        alpha.set(first, Field.REPORT_SEVERITY, FieldValue.Num(4))
        val second = bravo.newReport()

        network.connect("tablet-a", "tablet-b")
        settle()

        assertEquals(alpha.events(), bravo.events())
        assertEquals(alpha.state.value, bravo.state.value)
        assertEquals(setOf(first, second), bravo.state.value.reports.map { it.report }.toSet())
        val summary = network.syncs.getValue("tablet-b").summaries.value.getValue("tablet-a")
        assertEquals(PeerSync.Summary("tablet-a", sent = 1, received = 2), summary)
    }

    @Test
    fun `a change made while connected is passed on right away`() = runBlocking {
        val network = Network(this)
        val alpha = network.tablet("tablet-a")
        val bravo = network.tablet("tablet-b")
        network.connect("tablet-a", "tablet-b")
        settle()

        val report = alpha.newReport()
        alpha.set(report, Field.REPORT_DESCRIPTION, FieldValue.Text("Smoke over the dunes"))
        settle()

        assertEquals("Smoke over the dunes", bravo.state.value.reports.single().description)
        assertEquals(alpha.events(), bravo.events())
    }

    @Test
    fun `a tablet passes on what it received to a tablet that never met the author`() = runBlocking {
        val network = Network(this)
        val alpha = network.tablet("tablet-a")
        network.tablet("tablet-b")
        val charlie = network.tablet("tablet-c")
        network.connect("tablet-a", "tablet-b")
        network.connect("tablet-b", "tablet-c")
        settle()

        val report = alpha.newReport()
        settle()

        assertEquals(listOf(report), charlie.state.value.reports.map { it.report })
        val toAlpha = network.syncs.getValue("tablet-a").summaries.value.getValue("tablet-b")
        // bravo does not echo alpha's own event back to it
        assertEquals(0, toAlpha.duplicates)
    }

    @Test
    fun `a conflict found in a meeting is settled on both tablets`() = runBlocking {
        val network = Network(this)
        val alpha = network.tablet("tablet-a")
        val bravo = network.tablet("tablet-b")
        val report = alpha.newReport()
        network.connect("tablet-a", "tablet-b")
        settle()
        network.disconnect("tablet-a", "tablet-b")

        // apart again, both write the description
        alpha.set(report, Field.REPORT_DESCRIPTION, FieldValue.Text("Fire crossed the ridge"))
        bravo.set(report, Field.REPORT_DESCRIPTION, FieldValue.Text("Evacuation route B blocked"))
        settle()
        network.connect("tablet-a", "tablet-b")
        settle()

        val shown = bravo.state.value.conflicts.getValue(report).getValue(Field.REPORT_DESCRIPTION).map { it.eventId() }.toSet()
        assertEquals(2, shown.size)
        bravo.write {
            resolveConflict(RecordRef.report(report), Field.REPORT_DESCRIPTION, FieldValue.Text("Ridge crossed, route B blocked"), shown)
        }
        settle()

        assertEquals("Ridge crossed, route B blocked", alpha.state.value.reports.single().description)
        assertEquals(emptyMap(), alpha.state.value.conflicts)
        assertEquals(alpha.state.value, bravo.state.value)
    }

}
