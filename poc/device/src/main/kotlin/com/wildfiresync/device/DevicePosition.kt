package com.wildfiresync.device

import com.wildfiresync.core.schema.FieldValues.Location

/** mocked for the poc; a real tablet would read its gps position here */
object DevicePosition {

    fun current(): Location = Location(52.12, 5.84)

}
