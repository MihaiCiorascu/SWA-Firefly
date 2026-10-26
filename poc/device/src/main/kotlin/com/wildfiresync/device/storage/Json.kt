package com.wildfiresync.device.storage

import com.wildfiresync.core.StorageJson
import com.wildfiresync.core.schema.CausalContext
import java.util.UUID

/**
 * the json in the causal_context and supersedes columns.
 *
 * the schema only says those columns are text, so the format lives in core and the server writes the
 * same bytes. this is here so the storage layer does not reach past the repositories for it
 */
internal object Json {

    fun write(context: CausalContext): String = StorageJson.writeContext(context)

    fun write(ids: Set<UUID>): String = StorageJson.writeIds(ids)

    fun readCausalContext(json: String): CausalContext = StorageJson.readContext(json)

    fun readUuids(json: String): Set<UUID> = StorageJson.readIds(json)

}
