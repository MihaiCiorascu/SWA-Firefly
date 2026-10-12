package com.wildfiresync.experiments;

/**
 * What one sync cost.
 *
 * @param bytesSent bytes of serialised events and contexts that crossed the link, both directions together
 * @param eventsSent events that crossed the link, both directions together
 * @param duplicatesReceived events that arrived at a tablet that already held them
 * @param roundTrips number of times a message had to wait for an answer before the sync could finish
 * @param nanos time to decide what to send and apply it to both in-memory logs
 * @param converged both tablets hold exactly the same events afterwards
 */
public record SyncResult(long bytesSent, long eventsSent, long duplicatesReceived, int roundTrips, long nanos,
		boolean converged) {
}
