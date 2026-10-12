package com.wildfiresync.experiments;

import com.wildfiresync.core.SetSeverity;

/** An operation together with the size of the JSON the device sends for it. */
public record WireEvent(SetSeverity operation, int wireBytes) {
}
