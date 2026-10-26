package com.wildfiresync.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import com.wildfiresync.core.EventBundle;
import com.wildfiresync.core.NodeId;
import com.wildfiresync.core.OpId;
import com.wildfiresync.core.RecordRef;
import com.wildfiresync.core.Zone;
import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.CreateValues;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import com.wildfiresync.core.schema.FieldValues;
import com.wildfiresync.server.nats.ReportPublisher;
import com.wildfiresync.server.store.Database;
import com.wildfiresync.server.store.EventStore;
import com.wildfiresync.server.store.Projections;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * The server's half of the merge, against a real Postgres.
 *
 * <p>No Spring context: the broker is a mock and the rest is three objects, so this needs Docker and
 * nothing else. It is the test that says the server stores and merges rather than logging, which
 * {@code ServerApplicationTests} cannot tell you because it only asks whether the context starts.
 */
class MergeServiceTests {

	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

	private static DataSource dataSource;

	private final ReportPublisher publisher = mock(ReportPublisher.class);

	private Database database;

	private MergeService merge;

	@BeforeAll
	static void startPostgres() {
		POSTGRES.start();
		PGSimpleDataSource source = new PGSimpleDataSource();
		source.setUrl(POSTGRES.getJdbcUrl());
		source.setUser(POSTGRES.getUsername());
		source.setPassword(POSTGRES.getPassword());
		dataSource = source;
		Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/postgres").load().migrate();
	}

	@AfterAll
	static void stopPostgres() {
		POSTGRES.stop();
	}

	@BeforeEach
	void emptyTables() throws SQLException {
		database = new Database(dataSource);
		merge = new MergeService(database, new EventStore(), new Projections(), publisher);
		reset(publisher);
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("TRUNCATE field_conflicts, events, reports, incidents")) {
			statement.executeUpdate();
		}
	}

	@Test
	void aReportIsBuiltFromItsEvents() throws SQLException {
		UUID report = UUID.randomUUID();
		merge.apply(create(report, 1));
		merge.apply(set(report, 2, Field.REPORT_SEVERITY, new FieldValue.Num(4)));

		assertEquals(4, column(report, "severity"));
		assertEquals(2, events(report));
	}

	@Test
	void theSameEventTwiceIsStoredOnce() throws SQLException {
		UUID report = UUID.randomUUID();
		Event created = create(report, 1);

		merge.apply(created);
		merge.apply(created);

		assertEquals(1, events(report));
	}

	@Test
	void twoCrewsDisagreeingLeavesTheColumnEmptyAndListsTheCandidates() throws SQLException {
		UUID report = UUID.randomUUID();
		merge.apply(create(report, 1));
		merge.apply(set(report, 2, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge")));
		merge.apply(otherTablet(report, 1, Field.REPORT_DESCRIPTION, new FieldValue.Text("Route B blocked")));

		assertNull(column(report, "description"));
		assertEquals(2, conflicts(report));
	}

	@Test
	void settlingAConflictFillsTheColumnAgain() throws SQLException {
		UUID report = UUID.randomUUID();
		merge.apply(create(report, 1));
		Event mine = set(report, 2, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		Event theirs = otherTablet(report, 1, Field.REPORT_DESCRIPTION, new FieldValue.Text("Route B blocked"));
		merge.apply(mine);
		merge.apply(theirs);

		merge.apply(resolve(report, 3, Set.of(mine.eventId(), theirs.eventId())));

		assertEquals("Ridge crossed, route B blocked", column(report, "description"));
		assertEquals(0, conflicts(report));
	}

	@Test
	void aReportWithALocationIsPublishedToItsZone() {
		UUID report = UUID.randomUUID();
		merge.apply(create(report, 1));
		merge.apply(set(report, 2, Field.REPORT_LOCATION,
				new FieldValue.Place(new FieldValues.Location(52.12, 5.84))));

		ArgumentCaptor<EventBundle> captor = ArgumentCaptor.forClass(EventBundle.class);
		verify(publisher).publish(eq(Zone.of(52.12, 5.84)), captor.capture());
		assertEquals(report, captor.getValue().recordId());
		// the whole record goes out, not just the event that arrived
		assertEquals(2, captor.getValue().events().size());
	}

	@Test
	void aReportWithNoSettledLocationIsNotPublished() {
		UUID report = UUID.randomUUID();

		merge.apply(create(report, 1));

		verify(publisher, never()).publish(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void aReportThatMovesZoneIsWithdrawnFromTheOldOne() {
		UUID report = UUID.randomUUID();
		merge.apply(create(report, 1));
		merge.apply(set(report, 2, Field.REPORT_LOCATION,
				new FieldValue.Place(new FieldValues.Location(52.12, 5.84))));
		merge.apply(set(report, 3, Field.REPORT_LOCATION,
				new FieldValue.Place(new FieldValues.Location(51.50, 4.20))));

		verify(publisher).withdraw(Zone.of(52.12, 5.84), report);
		verify(publisher).publish(eq(Zone.of(51.50, 4.20)), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void anIncidentIsMergedButNeverPublished() throws SQLException {
		UUID incident = UUID.randomUUID();

		merge.apply(new Event(UUID.randomUUID(), incident, Event.RecordType.INCIDENT, Event.Operation.CREATE,
				null, CreateValues.format(Event.RecordType.INCIDENT, Map.of(
						Field.INCIDENT_NAME, new FieldValue.Text("Veluwe fire"),
						Field.INCIDENT_STATUS, new FieldValue.Status(FieldValues.IncidentStatus.ACTIVE))),
				"crew-tablet-a", NOW, new OpId(TABLET_A, 1), CausalContext.EMPTY, Set.of()));

		assertEquals("Veluwe fire", incidentName(incident));
		verify(publisher, never()).publish(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.any());
	}

	private static final NodeId TABLET_A = new NodeId("tablet-a");

	private static final NodeId TABLET_B = new NodeId("tablet-b");

	// Postgres stores timestamptz to the microsecond, so a whole second round trips unchanged
	private static final Instant NOW = Instant.parse("2026-10-20T08:00:00Z");

	private static Event create(UUID report, long seq) {
		return new Event(UUID.randomUUID(), report, Event.RecordType.REPORT, Event.Operation.CREATE, null,
				CreateValues.format(Event.RecordType.REPORT,
						Map.of(Field.REPORT_INCIDENT_ID, new FieldValue.Reference(UUID.randomUUID()))),
				"crew-tablet-a", NOW, new OpId(TABLET_A, seq), CausalContext.EMPTY, Set.of());
	}

	private static Event set(UUID report, long seq, Field field, FieldValue value) {
		return new Event(UUID.randomUUID(), report, Event.RecordType.REPORT, Event.Operation.SET_FIELD, field.key(),
				FieldValues.format(value), "crew-tablet-a", NOW, new OpId(TABLET_A, seq),
				new CausalContext(Map.of(TABLET_A, seq - 1)), Set.of());
	}

	private static Event otherTablet(UUID report, long seq, Field field, FieldValue value) {
		return new Event(UUID.randomUUID(), report, Event.RecordType.REPORT, Event.Operation.SET_FIELD, field.key(),
				FieldValues.format(value), "crew-tablet-b", NOW, new OpId(TABLET_B, seq), CausalContext.EMPTY,
				Set.of());
	}

	private static Event resolve(UUID report, long seq, Set<UUID> supersedes) {
		return new Event(UUID.randomUUID(), report, Event.RecordType.REPORT, Event.Operation.RESOLVE_CONFLICT,
				Field.REPORT_DESCRIPTION.key(), "Ridge crossed, route B blocked", "crew-lead", NOW,
				new OpId(TABLET_A, seq), new CausalContext(Map.of(TABLET_A, seq - 1, TABLET_B, 1L)), supersedes);
	}

	private Object column(UUID report, String name) throws SQLException {
		return single("SELECT " + name + " FROM reports WHERE report_id = ?", report);
	}

	private Object incidentName(UUID incident) throws SQLException {
		return single("SELECT name FROM incidents WHERE incident_id = ?", incident);
	}

	private int events(UUID record) throws SQLException {
		return ((Number) single("SELECT count(*) FROM events WHERE record_id = ?", record)).intValue();
	}

	private int conflicts(UUID record) throws SQLException {
		return ((Number) single("SELECT count(*) FROM field_conflicts WHERE record_id = ?", record)).intValue();
	}

	private Object single(String sql, UUID id) throws SQLException {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setObject(1, id);
			try (ResultSet rows = statement.executeQuery()) {
				assertTrue(rows.next(), "no row for " + id);
				return rows.getObject(1);
			}
		}
	}

}
