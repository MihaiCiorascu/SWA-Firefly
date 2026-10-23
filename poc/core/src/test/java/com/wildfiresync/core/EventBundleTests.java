package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventBundleTests {

    private static final RecordRef REPORT = RecordRef.report(new UUID(9, 1));

    private static Event event(String node, long seq, String value) {
        return new Event(new UUID(seq, seq), REPORT.id(), REPORT.type(), Event.Operation.SET_FIELD,
                Field.REPORT_DESCRIPTION.key(), value, "crew-" + node, Instant.parse("2026-10-20T08:00:00Z"),
                new OpId(new NodeId(node), seq), CausalContext.EMPTY, Set.of());
    }

    @Test
    void aBundleCarriesEveryEventOfTheRecord() {
        List<Event> events = List.of(event("tablet-a", 1, "Smoke over the dunes"), event("tablet-b", 1, "Wind north"));

        EventBundle bundle = EventBundle.of(REPORT, events);

        assertEquals(REPORT, bundle.record());
        assertEquals(events, bundle.toEvents());
    }

    @Test
    void theSameEventsInAnyOrderGiveTheSameBundle() {
        Event a = event("tablet-a", 1, "Smoke over the dunes");
        Event b = event("tablet-a", 2, "Smoke thinning");
        Event c = event("tablet-b", 1, "Wind north");

        assertEquals(EventBundle.of(REPORT, List.of(a, b, c)), EventBundle.of(REPORT, List.of(c, b, a)));
        assertEquals(List.of(a, b, c), EventBundle.of(REPORT, List.of(c, b, a)).toEvents());
    }

    @Test
    void anEmptyBundleIsAllowedSoARecordCanBeWithdrawn() {
        assertEquals(List.of(), EventBundle.of(REPORT, List.of()).toEvents());
    }

    @Test
    void aBundleHoldsOneRecordOnly() {
        Event elsewhere = new Event(new UUID(4, 4), new UUID(9, 2), Event.RecordType.REPORT,
                Event.Operation.SET_FIELD, Field.REPORT_DESCRIPTION.key(), "Another report", "crew-tablet-a",
                Instant.parse("2026-10-20T08:00:00Z"), new OpId(new NodeId("tablet-a"), 3), CausalContext.EMPTY,
                Set.of());

        assertThrows(IllegalArgumentException.class,
                () -> EventBundle.of(REPORT, List.of(event("tablet-a", 1, "Smoke"), elsewhere)));
    }

}
