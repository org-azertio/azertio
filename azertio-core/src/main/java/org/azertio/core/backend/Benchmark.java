package org.azertio.core.backend;

import org.azertio.core.Clock;
import org.azertio.core.execution.ExecutionNodeStats;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Benchmark {

	private final Integer totalExecutions;
	private final Integer numThreads;
	private final Clock clock;
	private final AtomicInteger currentExecutions = new AtomicInteger(0);
	private final ConcurrentHashMap<Integer, Instant> startTimes = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, Integer> executionTimes = new ConcurrentHashMap<>();
	private final AtomicInteger errors = new AtomicInteger(0);

	public Benchmark(Integer totalExecutions, Integer numThreads, Clock clock) {
		this.totalExecutions = totalExecutions;
		this.numThreads = numThreads;
		this.clock = clock;
	}

	public int totalExecutions() {
		return totalExecutions;
	}

	public int currentExecutions() {
		return currentExecutions.get();
	}

	public int numThreads() {
		return numThreads;
	}

	public int markStarted() {
		int executionNumber = currentExecutions.incrementAndGet();
		startTimes.put(executionNumber, clock.now());
		return executionNumber;
	}

	public void markFinished(int executionNumber, boolean error) {
		Instant start = startTimes.remove(executionNumber);
		int elapsed = start != null ? (int) (clock.now().toEpochMilli() - start.toEpochMilli()) : 0;
		executionTimes.put(executionNumber, elapsed);
		if (error) {
			errors.incrementAndGet();
		}
	}

	public ExecutionNodeStats statistics() {
		int totalTime = executionTimes.values().stream().mapToInt(Integer::intValue).sum();
		int minTime = executionTimes.values().stream().mapToInt(Integer::intValue).min().orElse(0);
		int maxTime = executionTimes.values().stream().mapToInt(Integer::intValue).max().orElse(0);
		int averageTime = totalExecutions > 0 ? totalTime / totalExecutions : 0;
		int p50 = executionTimes.values().stream().mapToInt(Integer::intValue).sorted().skip(totalExecutions / 2).findFirst().orElse(0);
		int p95 = executionTimes.values().stream().mapToInt(Integer::intValue).sorted().skip((long) (totalExecutions * 0.95)).findFirst().orElse(0);
		int p99 = executionTimes.values().stream().mapToInt(Integer::intValue).sorted().skip((long) (totalExecutions * 0.99)).findFirst().orElse(0);
		double throughput = totalExecutions / (totalTime / 1000.0);
		double errorRate = totalExecutions > 0 ? (double) errors.get() / totalExecutions : 0.0;
		return new ExecutionNodeStats(
			totalExecutions,
			numThreads,
			minTime,
			maxTime,
			averageTime,
			p50,
			p95,
			p99,
			throughput,
			errorRate
		);
	}
}