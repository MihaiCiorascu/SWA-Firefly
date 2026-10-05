package com.wildfiresync.android

import android.content.Context
import java.util.UUID

/**
 * A stable, per-install name for this device, used as the endpoint name Nearby
 * Connections advertises. This is a placeholder for the report's real Device
 * Identity component, just enough to show a human-readable name in the UI now.
 */
object DeviceIdentity {

    private const val PREFS_NAME = "wildfiresync_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) {
            return existing
        }

        val generated = "crew-" + UUID.randomUUID().toString().take(8)
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }
}
