package com.wildfiresync.server.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.wildfiresync.core.NodeId;
import com.wildfiresync.core.OpId;
import com.wildfiresync.core.StorageJson;
import com.wildfiresync.core.schema.Event;

import org.springframework.stereotype.Component;

/** The events table, the only thing on the server that is not derived from something else. */
@Component
public class EventStore {

	private static final String COLUMNS = "event_id, record_id, record_type, operation, field, value, actor_id, "
			+ "created_at, dot_device_id, dot_sequence, causal_context, supersedes";

	/**
	 * Stores the event unless this replica already holds it, and says which happened.
	 *
	 * <p>Nothing is overwritten. An event id already present, or another event already holding its dot,
	 * both leave the row that is there, so a redelivered message costs one no-op statement.
	 */
	public boolean insertIfAbsent(Connection connection, Event event) throws SQLException {
		String sql = "INSERT INTO events (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
				+ "ON CONFLICT DO NOTHING";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setObject(1, event.eventId());
			statement.setObject(2, event.recordId());
			statement.setString(3, event.recordType().name());
			statement.setString(4, event.operation().name());
			if (event.field() == null) {
				statement.setNull(5, Types.VARCHAR);
			}
			else {
				statement.setString(5, event.field());
			}
			statement.setString(6, event.value());
			statement.setString(7, event.actorId());
			statement.setObject(8, OffsetDateTime.ofInstant(event.createdAt(), ZoneOffset.UTC));
			statement.setString(9, event.dot().node().value());
			statement.setLong(10, event.dot().seq());
			statement.setString(11, StorageJson.writeContext(event.causalContext()));
			// the schema wants supersedes on resolutions only, every other event carries an empty set
			if (event.operation() == Event.Operation.RESOLVE_CONFLICT) {
				statement.setString(12, StorageJson.writeIds(event.supersedes()));
			}
			else {
				statement.setNull(12, Types.VARCHAR);
			}
			return statement.executeUpdate() == 1;
		}
	}

	/** Every event of one record, which is what the merge is computed from. */
	public List<Event> forRecord(Connection connection, UUID record) throws SQLException {
		String sql = "SELECT " + COLUMNS + " FROM events WHERE record_id = ? ORDER BY dot_device_id, dot_sequence";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setObject(1, record);
			try (ResultSet rows = statement.executeQuery()) {
				List<Event> events = new ArrayList<>();
				while (rows.next()) {
					events.add(toEvent(rows));
				}
				return events;
			}
		}
	}

	private static Event toEvent(ResultSet rows) throws SQLException {
		String supersedes = rows.getString("supersedes");
		return new Event(
				UUID.fromString(rows.getString("event_id")),
				UUID.fromString(rows.getString("record_id")),
				Event.RecordType.valueOf(rows.getString("record_type")),
				Event.Operation.valueOf(rows.getString("operation")),
				rows.getString("field"),
				rows.getString("value"),
				rows.getString("actor_id"),
				rows.getObject("created_at", OffsetDateTime.class).toInstant(),
				new OpId(new NodeId(rows.getString("dot_device_id")), rows.getLong("dot_sequence")),
				StorageJson.readContext(rows.getString("causal_context")),
				supersedes == null ? null : StorageJson.readIds(supersedes));
	}

}
