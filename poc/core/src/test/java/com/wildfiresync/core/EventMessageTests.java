package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventMessageTests {

    private static final UUID RECORD = new UUID(4, 1);

    private static final OpId DOT = new OpId(new NodeId("tablet-a"), 7);

    private static void roundTrips(Event event) {
        assertEquals(event, EventMessage.of(event).toEvent());
    }

    @Test
    void aFieldWriteRoundTrips() {
        roundTrips(new Event(new UUID(1, 1), RECORD, Event.RecordType.REPORT, Event.Operation.SET_FIELD,
                Field.REPORT_SEVERITY.key(), "7", "crew-1", Instant.parse("2026-10-04T09:00:00Z"), DOT,
                CausalContext.EMPTY, Set.of()));
    }

    @Test
    void aCreateRoundTripsWithItsInitialValues() {
        roundTrips(new Event(new UUID(1, 2), RECORD, Event.RecordType.REPORT, Event.Operation.CREATE, null,
                "{\"incidentId\":\"00000000-0000-0005-0000-000000000001\",\"severity\":\"3\"}", "crew-1", Instant.EPOCH,
                DOT, CausalContext.EMPTY, Set.of()));
    }

    @Test
    void aNonEmptyCausalContextRoundTrips() {
        CausalContext context = new CausalContext(Map.of(new NodeId("tablet-a"), 6L, new NodeId("tablet-b"), 3L));
        roundTrips(new Event(new UUID(1, 3), RECORD, Event.RecordType.REPORT, Event.Operation.SET_FIELD,
                Field.REPORT_DESCRIPTION.key(), "ridge", "crew-2", Instant.EPOCH, DOT, context, Set.of()));
    }

    @Test
    void aResolutionKeepsWhatItSuperseded() {
        roundTrips(new Event(new UUID(1, 4), RECORD, Event.RecordType.REPORT, Event.Operation.RESOLVE_CONFLICT,
                Field.REPORT_DESCRIPTION.key(), "agreed", "crew-lead", Instant.EPOCH, DOT, CausalContext.EMPTY,
                Set.of(new UUID(2, 1), new UUID(2, 2))));
    }

}

