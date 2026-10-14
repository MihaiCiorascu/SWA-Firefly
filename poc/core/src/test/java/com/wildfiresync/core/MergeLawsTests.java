package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import com.wildfiresync.core.schema.FieldValues.IncidentStatus;
import com.wildfiresync.core.schema.FieldValues.Location;
import com.wildfiresync.core.schema.FieldValues.ReportType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MergeLawsTests {

	private static final int SEEDS = 300;

	private static final RecordRef INCIDENT = RecordRef.incident(new UUID(5, 1));

	private static final List<RecordRef> RECORDS = List.of(INCIDENT, RecordRef.report(new UUID(5, 2)),
			RecordRef.report(new UUID(5, 3)));

	@Test
	void joinIsCommutative() {
		for (long seed = 1; seed <= SEEDS; seed++) {
			List<World> parts = thirds(generate(seed));
			assertEquals(parts.get(0).join(parts.get(1)), parts.get(1).join(parts.get(0)), "seed " + seed);
		}
	}

	@Test
	void joinIsAssociative() {
		for (long seed = 1; seed <= SEEDS; seed++) {
			List<World> p = thirds(generate(seed));
			assertEquals(p.get(0).join(p.get(1)).join(p.get(2)), p.get(0).join(p.get(1).join(p.get(2))),
					"seed " + seed);
		}
	}

	@Test
	void joinIsIdempotent() {
		for (long seed = 1; seed <= SEEDS; seed++) {
			World world = MergeCore.applyAll(World.EMPTY, generate(seed));
			assertEquals(world, world.join(world), "seed " + seed);
		}
	}

	@Test
	void deliveryOrderDoesNotMatter() {
		for (long seed = 1; seed <= SEEDS; seed++) {
			List<Event> events = generate(seed);
			World reference = MergeCore.applyAll(World.EMPTY, events);
			Random random = new Random(seed);
			for (int trial = 0; trial < 10; trial++) {
				List<Event> shuffled = new ArrayList<>(events);
				Collections.shuffle(shuffled, random);
				assertEquals(reference, MergeCore.applyAll(World.EMPTY, shuffled), "seed " + seed);
			}
		}
	}

	@Test
	void duplicatesAreHarmless() {
		for (long seed = 1; seed <= SEEDS; seed++) {
			List<Event> events = generate(seed);
			List<Event> noisy = new ArrayList<>(events);
			noisy.addAll(events);
			Collections.shuffle(noisy, new Random(seed));
			assertEquals(MergeCore.applyAll(World.EMPTY, events), MergeCore.applyAll(World.EMPTY, noisy),
					"seed " + seed);
		}
	}

	@Test
	void everyDeviceEndsWithTheSameWorld() {
		for (long seed = 1; seed <= SEEDS; seed++) {
			List<SimulatedDevice> devices = devices();
			List<Event> events = generate(seed, devices);
			Random random = new Random(seed);
			for (SimulatedDevice device : devices) {
				List<Event> shuffled = new ArrayList<>(events);
				Collections.shuffle(shuffled, random);
				device.receiveAll(shuffled);
			}
			World first = devices.getFirst().world();
			for (SimulatedDevice device : devices) {
				assertEquals(first, device.world(), "seed " + seed);
				assertEquals(device.world(), device.rebuildFromLog(), "seed " + seed);
			}
		}
	}

	private static List<SimulatedDevice> devices() {
		List<SimulatedDevice> devices = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			// Clocks are deliberately hours apart; the merge must not care.
			Instant clock = Instant.ofEpochSecond(1_790_000_000L + (i - 2) * 3_600L * 3);
			devices.add(new SimulatedDevice("tablet-" + (char) ('a' + i), i + 1, clock));
		}
		return devices;
	}

	private static List<Event> generate(long seed) {
		return generate(seed, devices());
	}


	private static List<Event> generate(long seed, List<SimulatedDevice> devices) {
		Random random = new Random(seed);
		List<Event> all = new ArrayList<>();
		for (int step = 0; step < 40; step++) {
			SimulatedDevice author = devices.get(random.nextInt(devices.size()));
			RecordRef record = RECORDS.get(random.nextInt(RECORDS.size()));

			if (random.nextInt(10) == 0) {
				all.add(author.create(record, initialValues(random, record)));
			}
			else if (random.nextInt(6) == 0 && resolvable(author, record).isPresent()) {
				Field field = resolvable(author, record).orElseThrow();
				Set<UUID> settled = new HashSet<>();
				author.resolution(record, field).map(Resolution.Conflicted.class::cast).orElseThrow()
						.candidates().forEach(candidate -> settled.add(candidate.eventId()));
				all.add(author.resolve(record, field, valueFor(random, field), settled));
			}
			else {
				Field field = pickField(random, record);
				all.add(author.set(record, field, valueFor(random, field)));
			}

			if (random.nextInt(3) == 0) {
				devices.get(random.nextInt(devices.size())).receiveAll(author.log());
			}
		}
		return all;
	}

	/** A MANUAL field the author currently sees in conflict, if any. */
	private static Optional<Field> resolvable(SimulatedDevice author, RecordRef record) {
		for (Field field : Field.values()) {
			if (field.recordType() == record.type() && field.policy() == Field.Policy.MANUAL
					&& author.resolution(record, field).orElse(null) instanceof Resolution.Conflicted) {
				return Optional.of(field);
			}
		}
		return Optional.empty();
	}

	private static Field pickField(Random random, RecordRef record) {
		List<Field> fields = new ArrayList<>();
		for (Field field : Field.values()) {
			if (field.recordType() == record.type() && field.policy() != Field.Policy.IMMUTABLE) {
				fields.add(field);
			}
		}
		return fields.get(random.nextInt(fields.size()));
	}

	private static Map<Field, FieldValue> initialValues(Random random, RecordRef record) {
		Map<Field, FieldValue> values = new EnumMap<>(Field.class);
		for (Field field : Field.values()) {
			if (field.recordType() == record.type() && field.policy() == Field.Policy.IMMUTABLE) {
				values.put(field, valueFor(random, field));
			}
		}
		if (random.nextBoolean()) {
			Field field = pickField(random, record);
			values.put(field, valueFor(random, field));
		}
		return values;
	}

	private static FieldValue valueFor(Random random, Field field) {
		return switch (field.kind()) {
			case NUMBER -> new FieldValue.Num(1 + random.nextInt(10));
			case TEXT -> new FieldValue.Text("text-" + random.nextInt(50));
			case STATUS -> new FieldValue.Status(IncidentStatus.values()[random.nextInt(3)]);
			case REPORT_TYPES -> FieldValue.Types.of(ReportType.values()[random.nextInt(3)]);
			case LOCATION -> new FieldValue.Place(new Location(random.nextInt(90), random.nextInt(180)));
			case REFERENCE -> new FieldValue.Reference(new UUID(5, random.nextInt(4)));
		};
	}

	private static List<World> thirds(List<Event> events) {
		int third = events.size() / 3;
		return List.of(MergeCore.applyAll(World.EMPTY, events.subList(0, third)),
				MergeCore.applyAll(World.EMPTY, events.subList(third, 2 * third)),
				MergeCore.applyAll(World.EMPTY, events.subList(2 * third, events.size())));
	}

}
