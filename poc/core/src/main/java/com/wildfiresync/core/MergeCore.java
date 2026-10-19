package com.wildfiresync.core;

import com.wildfiresync.core.schema.CreateValues;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import com.wildfiresync.core.schema.FieldValues;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The merging library main algorithm. Merges operations into current states known by every device.
 */
public final class MergeCore {

	private MergeCore() {
	}

	/**
	 * Merge one operation, as a join between the existing state and a new world containing just the op.
	 */
	public static World apply(World world, Event event) {
		return world.join(lift(event));
	}

	/** Applies a batch of operations. */
	public static World applyAll(World world, Iterable<Event> events) {
		World result = world;
		for (Event event : events) {
			result = apply(result, event);
		}
		return result;
	}

	/** Throws when the schema cannot represent the event, so it can be refused before it is stored. */
	public static void validate(Event event) {
		lift(event);
	}

	/** The smallest world in which this event holds. Rejects anything the schema cannot represent. */
	public static World lift(Event event) {
		RecordState state = switch (event.operation()) {
			case CREATE -> createState(event);
			case SET_FIELD -> fieldState(event);
			case RESOLVE_CONFLICT -> resolutionState(event);
		};
		Map<RecordRef, RecordState> records = new LinkedHashMap<>();
		records.put(new RecordRef(event.recordType(), event.recordId()), state);
		return new World(records);
	}

	private static RecordState createState(Event event) {
		Map<Field, FieldState> fields = new EnumMap<>(Field.class);
		CreateValues.parse(event.recordType(), event.value())
				.forEach((field, value) -> fields.put(field, assignment(field, value, event)));
		return new RecordState(true, fields);
	}

	private static RecordState fieldState(Event event) {
		Field field = Field.of(event.recordType(), event.field());
		if (field.policy() == Field.Policy.IMMUTABLE) {
			throw new IllegalArgumentException(field + " is immutable, only the CREATE of the record sets it");
		}
		Map<Field, FieldState> fields = new EnumMap<>(Field.class);
		fields.put(field, assignment(field, FieldValues.parse(field, event.value()), event));
		return new RecordState(false, fields);
	}

	/**
	 * A resolution is one more candidate, and it settles the candidates it names in supersedes. A value it did not name,
	 * such as one written concurrently, stays, and so does a concurrent resolution, which is a new conflict.
	 */
	private static RecordState resolutionState(Event event) {
		Field field = Field.of(event.recordType(), event.field());
		if (field.policy() != Field.Policy.MANUAL) {
			throw new IllegalArgumentException(field + " merges automatically, only a MANUAL field has conflicts to resolve");
		}
		if (event.supersedes().isEmpty()) {
			throw new IllegalArgumentException("a RESOLVE_CONFLICT must name the candidates it settles");
		}
		if (event.supersedes().contains(event.eventId())) {
			throw new IllegalArgumentException("a RESOLVE_CONFLICT cannot settle itself");
		}
		Candidate candidate = new Candidate(FieldValues.parse(field, event.value()), event.eventId(), event.dot(),
				event.actorId(), event.createdAt());
		Map<Field, FieldState> fields = new EnumMap<>(Field.class);
		fields.put(field, new FieldState.Candidates(Set.of(candidate), event.supersedes()));
		return new RecordState(false, fields);
	}

	private static FieldState assignment(Field field, FieldValue value, Event event) {
		return switch (field.policy()) {
			case MAX -> new FieldState.MaxValue(((FieldValue.Num) value).value(), event.eventId());
			case UNION -> new FieldState.UnionValue(((FieldValue.Types) value).values());
			case IMMUTABLE, LARGEST_EVENT_ID, MANUAL -> FieldState.Candidates.of(new Candidate(value,
					event.eventId(), event.dot(), event.actorId(), event.createdAt()));
		};
	}

	/**
	 * Join two states of the same field.
	 *
	 * <p>A mismatch means two nodes disagree about the schema, which is a deployment fault rather
	 * than a merge outcome. Failing loudly is correct: silently choosing one shape would corrupt the
	 * record.
	 */
	static FieldState joinFields(Field field, FieldState a, FieldState b) {
		if (a instanceof FieldState.MaxValue left && b instanceof FieldState.MaxValue right) {
			return left.join(right);
		}
		if (a instanceof FieldState.UnionValue left && b instanceof FieldState.UnionValue right) {
			return left.join(right);
		}
		if (a instanceof FieldState.Candidates left && b instanceof FieldState.Candidates right) {
			return left.join(right);
		}
		throw new IllegalStateException("schema conflict on " + field + ": " + a.getClass().getSimpleName() + " vs "
				+ b.getClass().getSimpleName());
	}

	/** Interpret a merged field for a person, according to its policy. */
	public static Resolution resolve(Field field, FieldState state) {
		return switch (state) {
			case FieldState.MaxValue max ->
					new Resolution.Settled(new FieldValue.Num(max.value()), Optional.of(max.eventId()));
			case FieldState.UnionValue union ->
					new Resolution.Settled(new FieldValue.Types(union.values()), Optional.empty());
			case FieldState.Candidates candidates -> resolveCandidates(field, candidates);
		};
	}

	private static Resolution resolveCandidates(Field field, FieldState.Candidates candidates) {
		var ordered = candidates.ordered();
		if (ordered.isEmpty()) {
			// Only resolutions that settle each other get here. Keep every value rather than drop them all.
			return new Resolution.Conflicted(candidates.all());
		}
		if (ordered.size() == 1) {
			return settle(ordered.getFirst());
		}
		return switch (field.policy()) {
			// Deterministic, and therefore convergent, but arbitrary: one crew member wording is lost.
			case LARGEST_EVENT_ID -> settle(ordered.stream().max(Comparator.naturalOrder()).orElseThrow());
			// An immutable field should never have two values. If it somehow does, the first one stands.
			case IMMUTABLE -> settle(ordered.getFirst());
			default -> new Resolution.Conflicted(ordered);
		};
	}

	private static Resolution settle(Candidate candidate) {
		return new Resolution.Settled(candidate.value(), Optional.of(candidate.eventId()));
	}

}
