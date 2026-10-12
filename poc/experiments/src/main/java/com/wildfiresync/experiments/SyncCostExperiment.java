package com.wildfiresync.experiments;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Experiment 2 of the report: what a sync costs as the event log grows, for sending the whole log against sending
 * only what the other side's causal context does not cover.
 *
 * Writes every run to a CSV file (first argument, default build/sync-cost.csv) and prints a summary per combination.
 * The time column only covers deciding what to send and applying it to in-memory logs on this machine. Applying
 * events to the tablet's SQLite store is measured separately on a tablet.
 */
public final class SyncCostExperiment {

	static final int[] AUTHORS = {2, 10, 50};

	static final int[] EVENTS_PER_TABLET = {100, 1_000, 10_000, 100_000};

	static final double[] OVERLAPS = {0.0, 0.5, 0.9, 0.99};

	static final int SEEDS = 10;

	public static void main(String[] args) throws IOException {
		Path csv = Path.of(args.length > 0 ? args[0] : "build/sync-cost.csv");
		if (csv.getParent() != null) {
			Files.createDirectories(csv.getParent());
		}

		LogGenerator generator = new LogGenerator();
		warmUp(generator);

		List<String> summary = new ArrayList<>();
		try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(csv))) {
			out.println("authors,events_per_tablet,overlap,seed,strategy,size_a,size_b,bytes_sent,events_sent,"
					+ "duplicates_received,round_trips,millis,converged");
			for (int authors : AUTHORS) {
				for (int n : EVENTS_PER_TABLET) {
					for (double overlap : OVERLAPS) {
						Totals whole = new Totals();
						Totals delta = new Totals();
						for (long seed = 0; seed < SEEDS; seed++) {
							whole.add(run(out, generator, SyncStrategy.WHOLE_LOG, authors, n, overlap, seed));
							delta.add(run(out, generator, SyncStrategy.CAUSAL_CONTEXT, authors, n, overlap, seed));
						}
						summary.add(String.format(Locale.ROOT, "%7d %9d %7.0f%% | %12.1f %12.1f %7.1f%% | %10.0f | %9.2f %9.2f",
								authors, n, overlap * 100,
								whole.bytes() / 1024.0, delta.bytes() / 1024.0,
								100.0 * (whole.bytes() - delta.bytes()) / whole.bytes(),
								whole.duplicates(), whole.millis(), delta.millis()));
					}
				}
			}
		}

		System.out.println("Mean over " + SEEDS + " seeds per row. Bytes and events count both directions together.");
		System.out.println(" authors  events/tablet overlap |   whole KB     delta KB   saved |  dup events | whole ms  delta ms");
		summary.forEach(System.out::println);
		System.out.println("Raw runs: " + csv.toAbsolutePath());
	}

	private static SyncResult run(PrintWriter out, LogGenerator generator, SyncStrategy strategy, int authors, int n,
			double overlap, long seed) {
		LogGenerator.Pair pair = generator.generate(authors, n, overlap, seed);
		int sizeA = pair.a().size();
		int sizeB = pair.b().size();
		SyncResult result = strategy.sync(pair.a(), pair.b());
		out.printf(Locale.ROOT, "%d,%d,%.2f,%d,%s,%d,%d,%d,%d,%d,%d,%.3f,%b%n", authors, n, overlap, seed, strategy,
				sizeA, sizeB, result.bytesSent(), result.eventsSent(), result.duplicatesReceived(), result.roundTrips(),
				result.nanos() / 1_000_000.0, result.converged());
		if (!result.converged()) {
			throw new IllegalStateException("replicas did not converge: " + strategy + " " + authors + " " + n + " "
					+ overlap + " " + seed);
		}
		return result;
	}

	private static void warmUp(LogGenerator generator) {
		for (int i = 0; i < 20; i++) {
			for (SyncStrategy strategy : SyncStrategy.values()) {
				LogGenerator.Pair pair = generator.generate(10, 10_000, 0.5, i);
				strategy.sync(pair.a(), pair.b());
			}
		}
	}

	private static final class Totals {

		private double bytes;

		private double duplicates;

		private double millis;

		private int runs;

		void add(SyncResult result) {
			bytes += result.bytesSent();
			duplicates += result.duplicatesReceived();
			millis += result.nanos() / 1_000_000.0;
			runs++;
		}

		double bytes() {
			return bytes / runs;
		}

		double duplicates() {
			return duplicates / runs;
		}

		double millis() {
			return millis / runs;
		}

	}

}
