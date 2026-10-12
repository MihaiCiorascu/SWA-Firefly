package com.wildfiresync.core;

import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.Event;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** The JSON form of an event. Absent fields deserialize to null. */
public record EventMessage(UUID eventId, UUID recordId, Event.RecordType recordType, Event.Operation operation,
                           String field, String value, String actorId, Instant createdAt, String nodeId, long seq,
                           Map<String, Long> context, Set<UUID> supersedes) {

    public EventMessage {
        context = context == null ? Map.of() : Map.copyOf(context);
        supersedes = supersedes == null ? Set.of() : Set.copyOf(supersedes);
    }

    public static EventMessage of(Event event) {
        Objects.requireNonNull(event, "event");
        Map<String, Long> context = new LinkedHashMap<>();
        event.causalContext().counters().forEach((node, counter) -> context.put(node.value(), counter));
        return new EventMessage(event.eventId(), event.recordId(), event.recordType(), event.operation(),
                event.field(), event.value(), event.actorId(), event.createdAt(), event.dot().node().value(),
                event.dot().seq(), context, event.supersedes());
    }

    public Event toEvent() {
        Map<NodeId, Long> counters = new LinkedHashMap<>();
        context.forEach((node, counter) -> counters.put(new NodeId(node), counter));
        return new Event(eventId, recordId, recordType, operation, field, value, actorId, createdAt,
                new OpId(new NodeId(nodeId), seq), new CausalContext(counters), supersedes);
    }

}
