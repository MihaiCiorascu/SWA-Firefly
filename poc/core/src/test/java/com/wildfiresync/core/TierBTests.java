package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import com.wildfiresync.core.schema.FieldValues.IncidentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TierBTests {

	private static final RecordRef REPORT = RecordRef.report(new UUID(7, 1));

	private static final RecordRef INCIDENT = RecordRef.incident(new UUID(7, 2));

	private SimulatedDevice alpha;

	private SimulatedDevice bravo;

	@BeforeEach
	void twoDevicesOffline() {
		alpha = new SimulatedDevice("tablet-a", 1);
		bravo = new SimulatedDevice("tablet-b", 2);
	}

	private void reconnect() {
		List<Event> inFlight = new ArrayList<>(alpha.log());
		inFlight.addAll(bravo.log());
		alpha.receiveAll(inFlight);
		bravo.receiveAll(inFlight);
	}

	private Resolution.Conflicted conflictOn(SimulatedDevice device) {
		return assertInstanceOf(Resolution.Conflicted.class,
				device.resolution(REPORT, Field.REPORT_DESCRIPTION).orElseThrow());
	}

	@Test
	void concurrentDescriptionsBecomeAConflictEveryDeviceSeesIdentically() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Evacuation route B blocked"));

		reconnect();

		assertEquals(2, conflictOn(alpha).candidates().size());
		assertEquals(conflictOn(bravo).candidates(), conflictOn(alpha).candidates());
		// Tier B converges too: the devices agree exactly on which values are in conflict.
		assertEquals(alpha.world(), bravo.world());
	}

	@Test
	void aSingleValueNeedsNoResolving() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));

		reconnect();

		Resolution.Settled settled = assertInstanceOf(Resolution.Settled.class,
				bravo.resolution(REPORT, Field.REPORT_DESCRIPTION).orElseThrow());
		assertEquals(new FieldValue.Text("Fire crossed the ridge"), settled.value());
	}

	@Test
	void candidatesCarryWhoWroteThem() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Evacuation route B blocked"));

		reconnect();

		List<String> actors = conflictOn(alpha).candidates().stream().map(Candidate::actorId).toList();
		assertEquals(List.of("crew-tablet-a", "crew-tablet-b"), actors);
	}


	@Test
	void aWriterWhoSawTheOtherValueReplacesIt() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.receiveAll(alpha.log());
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge, wind turning"));

		reconnect();

		for (SimulatedDevice device : List.of(alpha, bravo)) {
			Resolution.Settled settled = assertInstanceOf(Resolution.Settled.class,
					device.resolution(REPORT, Field.REPORT_DESCRIPTION).orElseThrow());
			assertEquals(new FieldValue.Text("Fire crossed the ridge, wind turning"), settled.value());
		}
	}

	@Test
	void oneDeviceEditingTwiceNeverConflictsWithItself() {
		alpha.set(INCIDENT, Field.INCIDENT_STATUS, new FieldValue.Status(IncidentStatus.ACTIVE));
		alpha.set(INCIDENT, Field.INCIDENT_STATUS, new FieldValue.Status(IncidentStatus.CONTAINED));

		reconnect();

		Resolution.Settled settled = assertInstanceOf(Resolution.Settled.class,
				bravo.resolution(INCIDENT, Field.INCIDENT_STATUS).orElseThrow());
		assertEquals(new FieldValue.Status(IncidentStatus.CONTAINED), settled.value());
	}

	@Test
	void aWriterThatMissedPartOfAnotherDevicesLogDoesNotReplaceWhatItMissed() {
		alpha.set(REPORT, Field.REPORT_SEVERITY, new FieldValue.Num(3));
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		// bravo only got alpha's second event, so its context cannot claim it
		bravo.receive(alpha.log().get(1));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Evacuation route B blocked"));

		reconnect();

		assertEquals(2, conflictOn(alpha).candidates().size());
	}

	private static Set<UUID> idsOf(Resolution.Conflicted conflict) {
		return conflict.candidates().stream().map(Candidate::eventId).collect(Collectors.toSet());
	}

	@Test
	void aResolutionSettlesTheConflictOnEveryDevice() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Evacuation route B blocked"));
		reconnect();

		alpha.resolve(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge, route B blocked"),
				idsOf(conflictOn(alpha)));
		reconnect();

		for (SimulatedDevice device : List.of(alpha, bravo)) {
			Resolution.Settled settled = assertInstanceOf(Resolution.Settled.class,
					device.resolution(REPORT, Field.REPORT_DESCRIPTION).orElseThrow());
			assertEquals(new FieldValue.Text("Fire crossed the ridge, route B blocked"), settled.value());
		}
		assertEquals(alpha.world(), bravo.world());
	}

	@Test
	void concurrentResolutionsBecomeANewConflict() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Evacuation route B blocked"));
		reconnect();
		Set<UUID> original = idsOf(conflictOn(alpha));

		alpha.resolve(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Ridge crossed"), original);
		bravo.resolve(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Route B blocked"), original);
		reconnect();

		List<FieldValue> values = conflictOn(alpha).candidates().stream().map(Candidate::value).toList();
		assertEquals(List.of(new FieldValue.Text("Ridge crossed"), new FieldValue.Text("Route B blocked")), values);
		assertEquals(alpha.world(), bravo.world());
	}

	@Test
	void aValueTheResolutionDidNotSeeStaysInConflict() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Evacuation route B blocked"));
		reconnect();

		alpha.resolve(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Agreed wording"), idsOf(conflictOn(alpha)));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Wind turning north"));
		reconnect();

		List<FieldValue> values = conflictOn(bravo).candidates().stream().map(Candidate::value).toList();
		assertEquals(List.of(new FieldValue.Text("Agreed wording"), new FieldValue.Text("Wind turning north")), values);
	}

	@Test
	void onlyAManualFieldCanBeResolved() {
		Event resolution = resolution(Field.REPORT_SEVERITY.key(), "5", Set.of(new UUID(1, 1)));

		assertThrows(IllegalArgumentException.class, () -> MergeCore.lift(resolution));
	}

	@Test
	void aResolutionMustNameWhatItSettles() {
		Event resolution = resolution(Field.REPORT_DESCRIPTION.key(), "agreed wording", Set.of());

		assertThrows(IllegalArgumentException.class, () -> MergeCore.validate(resolution));
	}

	@Test
	void aResolutionCannotSettleItself() {
		Event resolution = resolution(Field.REPORT_DESCRIPTION.key(), "agreed wording", Set.of(new UUID(3, 1)));

		assertThrows(IllegalArgumentException.class, () -> MergeCore.lift(resolution));
	}

	private static Event resolution(String field, String value, Set<UUID> supersedes) {
		return new Event(new UUID(3, 1), REPORT.id(), REPORT.type(), Event.Operation.RESOLVE_CONFLICT, field, value,
				"crew-lead", Instant.EPOCH, new OpId(new NodeId("tablet-c"), 1), CausalContext.EMPTY, supersedes);
	}

	@Test
	void aCreateEventCarriesNoField() {
		assertThrows(IllegalArgumentException.class,
				() -> new Event(new UUID(3, 2), REPORT.id(), REPORT.type(), Event.Operation.CREATE, "severity", "5",
						"crew-lead", Instant.EPOCH, new OpId(new NodeId("tablet-c"), 2), CausalContext.EMPTY,
						Set.of()));
	}

}
