package com.wildfiresync.device.experiment

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.wildfiresync.device.P2pManager
import com.wildfiresync.device.storage.LocalDatabase
import java.io.File

/** Everything the experiment tab needs, wired to the P2P manager. */
class ExperimentUi(
    val runner: ExperimentRunner,
    val store: ExperimentStore,
    val settings: ExperimentSettings,
    val rows: List<String>,
    private val database: LocalDatabase,
) {

    fun close() = database.close()

}

/**
 * Every row is added to p2p-experiment.csv in the app's files folder. For a debug build it can be read with
 * `adb exec-out run-as com.wildfiresync.device cat files/p2p-experiment.csv`.
 */
fun createExperiment(context: Context, deviceName: String, manager: P2pManager): ExperimentUi {
    val database = LocalDatabase.open(context, "p2p-experiment.db")
    val store = ExperimentStore(database)
    val settings = ExperimentSettings()
    val rows = mutableStateListOf<String>()
    val file = File(context.filesDir, "p2p-experiment.csv")

    val runner = ExperimentRunner(
        store = store,
        transport = { manager.sendTracked(it) },
        connectMillis = { manager.connectMillis },
        note = { settings.note },
        log = { row ->
            if (!file.exists()) {
                file.writeText(ExperimentRunner.Row.CSV_HEADER + "\n")
            }
            file.appendText(row.csv(deviceName) + "\n")
            rows += "${row.role} ${row.run} #${row.attempt} n=${row.total} b=${row.batch}: ${row.transferMillis} ms, " +
                "stored ${row.stored}, duplicates ${row.duplicates}, lost ${row.lost}, " +
                if (row.complete) "complete" else "incomplete"
        },
    )

    manager.experimentHandler = { _, bytes -> runner.handle(bytes) }
    manager.onTransferEnded = runner::onTransferUpdate
    return ExperimentUi(runner, store, settings, rows, database)
}
