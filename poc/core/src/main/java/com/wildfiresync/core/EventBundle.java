package com.wildfiresync.core;

import com.wildfiresync.core.schema.Event;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Every event of one record, which is what the server publishes for a report.
 *
 * The events are ordered by dot so the same set always produces the same bytes.
 */
public record EventBundle(UUID recordId, Event.RecordType recordType, List<EventMessage> events) {

    private static final Comparator<EventMessage> BY_DOT =
            Comparator.comparing(EventMessage::nodeId).thenComparingLong(EventMessage::seq);

    public EventBundle {
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(recordType, "recordType");
        events = events == null ? List.of() : events.stream().sorted(BY_DOT).toList();
    }

    public static EventBundle of(RecordRef record, List<Event> events) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(events, "events");
        if (events.stream().anyMatch(event -> !event.recordId().equals(record.id()))) {
            throw new IllegalArgumentException("a bundle holds the events of one record only");
        }
        return new EventBundle(record.id(), record.type(), events.stream().map(EventMessage::of).toList());
    }

    public List<Event> toEvents() {
        return events.stream().map(EventMessage::toEvent).toList();
    }

    public RecordRef record() {
        return new RecordRef(recordType, recordId);
    }

}
