package com.wildfiresync.server.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.wildfiresync.core.Candidate;
import com.wildfiresync.core.MergeCore;
import com.wildfiresync.core.RecordRef;
import com.wildfiresync.core.RecordState;
import com.wildfiresync.core.Resolution;
import com.wildfiresync.core.World;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import com.wildfiresync.core.schema.FieldValues;

import org.springframework.stereotype.Component;

/**
 * The incidents, reports and field_conflicts tables, rebuilt from a record's events.
 *
 * <p>A column holds a value only while the merge has settled it. A field nobody has written yet and a
 * field two crews disagree about both read as NULL, and the second case lists its candidates in
 * field_conflicts. That is the same rule the tablet's projections follow, so a query against either
 * replica answers the same way.
 */
@Component
public class Projections {

	/** Rewrites the record's rows and gives back its settled location, when it has one. */
	public Optional<FieldValues.Location> write(Connection connection, RecordRef record, World world)
			throws SQLException {
		RecordState state = world.record(record).orElse(RecordState.EMPTY);
		Map<Field, FieldValue> settled = new EnumMap<>(Field.class);
		Map<Field, List<Candidate>> conflicts = new EnumMap<>(Field.class);
		state.fields().forEach((field, merged) -> {
			switch (MergeCore.resolve(field, merged)) {
				case Resolution.Settled it -> settled.put(field, it.value());
				case Resolution.Conflicted it -> conflicts.put(field, it.candidates());
			}
		});

		switch (record.type()) {
			case REPORT -> upsertReport(connection, record.id(), settled);
			case INCIDENT -> upsertIncident(connection, record.id(), settled);
		}
		replaceConflicts(connection, record.id(), conflicts);

		return Optional.ofNullable(settled.get(Field.REPORT_LOCATION))
				.map(value -> ((FieldValue.Place) value).value());
	}

	/** The location the last merge settled on, so a report that moved can be withdrawn from its old zone. */
	public Optional<FieldValues.Location> locationOf(Connection connection, UUID report) throws SQLException {
		try (PreparedStatement statement = connection
				.prepareStatement("SELECT latitude, longitude FROM reports WHERE report_id = ?")) {
			statement.setObject(1, report);
			try (ResultSet rows = statement.executeQuery()) {
				if (!rows.next()) {
					return Optional.empty();
				}
				double latitude = rows.getDouble("latitude");
				if (rows.wasNull()) {
					return Optional.empty();
				}
				double longitude = rows.getDouble("longitude");
				// the schema keeps the pair whole, so a latitude without a longitude cannot be stored
				return rows.wasNull() ? Optional.empty()
						: Optional.of(new FieldValues.Location(latitude, longitude));
			}
		}
	}

	private void upsertReport(Connection connection, UUID report, Map<Field, FieldValue> settled)
			throws SQLException {
		String sql = """
				INSERT INTO reports (report_id, incident_id, report_types, latitude, longitude, severity, description)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				ON CONFLICT (report_id) DO UPDATE SET
				    incident_id = excluded.incident_id, report_types = excluded.report_types,
				    latitude = excluded.latitude, longitude = excluded.longitude,
				    severity = excluded.severity, description = excluded.description
				""";
		FieldValues.Location location = settled.get(Field.REPORT_LOCATION) instanceof FieldValue.Place place
				? place.value() : null;
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setObject(1, report);
			setUuid(statement, 2, settled.get(Field.REPORT_INCIDENT_ID) instanceof FieldValue.Reference reference
					? reference.value() : null);
			setText(statement, 3, settled.containsKey(Field.REPORT_TYPE)
					? FieldValues.format(settled.get(Field.REPORT_TYPE)) : null);
			setDouble(statement, 4, location == null ? null : location.latitude());
			setDouble(statement, 5, location == null ? null : location.longitude());
			setInt(statement, 6, settled.get(Field.REPORT_SEVERITY) instanceof FieldValue.Num num
					? num.value() : null);
			setText(statement, 7, settled.get(Field.REPORT_DESCRIPTION) instanceof FieldValue.Text text
					? text.value() : null);
			statement.executeUpdate();
		}
	}

	private void upsertIncident(Connection connection, UUID incident, Map<Field, FieldValue> settled)
			throws SQLException {
		String sql = """
				INSERT INTO incidents (incident_id, name, status) VALUES (?, ?, ?)
				ON CONFLICT (incident_id) DO UPDATE SET name = excluded.name, status = excluded.status
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setObject(1, incident);
			setText(statement, 2, settled.get(Field.INCIDENT_NAME) instanceof FieldValue.Text text
					? text.value() : null);
			setText(statement, 3, settled.containsKey(Field.INCIDENT_STATUS)
					? FieldValues.format(settled.get(Field.INCIDENT_STATUS)) : null);
			statement.executeUpdate();
		}
	}

	/**
	 * Replaces every conflict row of the record. All of its rows are rewritten rather than only the
	 * fields now in conflict, so a field that has just been settled loses its candidates.
	 */
	private void replaceConflicts(Connection connection, UUID record, Map<Field, List<Candidate>> conflicts)
			throws SQLException {
		try (PreparedStatement delete = connection
				.prepareStatement("DELETE FROM field_conflicts WHERE record_id = ?")) {
			delete.setObject(1, record);
			delete.executeUpdate();
		}
		if (conflicts.isEmpty()) {
			return;
		}
		try (PreparedStatement insert = connection.prepareStatement(
				"INSERT INTO field_conflicts (record_id, field, event_id) VALUES (?, ?, ?)")) {
			for (Map.Entry<Field, List<Candidate>> entry : conflicts.entrySet()) {
				for (Candidate candidate : entry.getValue()) {
					insert.setObject(1, record);
					insert.setString(2, entry.getKey().key());
					insert.setObject(3, candidate.eventId());
					insert.addBatch();
				}
			}
			insert.executeBatch();
		}
	}

	private static void setUuid(PreparedStatement statement, int index, UUID value) throws SQLException {
		if (value == null) {
			// an untyped null, which Postgres reads as the column's own type
			statement.setNull(index, Types.OTHER);
		}
		else {
			statement.setObject(index, value);
		}
	}

	private static void setText(PreparedStatement statement, int index, String value) throws SQLException {
		if (value == null) {
			statement.setNull(index, Types.VARCHAR);
		}
		else {
			statement.setString(index, value);
		}
	}

	private static void setDouble(PreparedStatement statement, int index, Double value) throws SQLException {
		if (value == null) {
			statement.setNull(index, Types.DOUBLE);
		}
		else {
			statement.setDouble(index, value);
		}
	}

	private static void setInt(PreparedStatement statement, int index, Integer value) throws SQLException {
		if (value == null) {
			statement.setNull(index, Types.INTEGER);
		}
		else {
			statement.setInt(index, value);
		}
	}

}
