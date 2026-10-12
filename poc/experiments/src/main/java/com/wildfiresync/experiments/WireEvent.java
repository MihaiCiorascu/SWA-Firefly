package com.wildfiresync.experiments;

import com.wildfiresync.core.schema.Event;

/** An event together with the size of the JSON the device sends for it. */
public record WireEvent(Event event, int wireBytes) {
}
