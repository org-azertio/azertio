package org.azertio.core.backend;

import org.azertio.core.AzertioRuntime;
import org.azertio.core.execution.ExecutionNodeStats;
import org.azertio.core.persistence.AttachmentRepository;
import org.azertio.core.persistence.TestExecutionRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public class ExecutionContext {

	private static final ThreadLocal<ExecutionContext> threadLocal = new ThreadLocal<>();

	public static ExecutionContext current() {
		return threadLocal.get();
	}

	static void setCurrent(ExecutionContext executionContext) {
		threadLocal.set(executionContext);
	}

	static void clearCurrent() {
		threadLocal.remove();
	}

	public static Runnable withCurrent(Runnable task) {
		ExecutionContext ctx = threadLocal.get();
		return () -> {
			threadLocal.set(ctx);
			try {
				task.run();
			} finally {
				threadLocal.remove();
			}
		};
	}




	private final Map<String,String> variables = new ConcurrentHashMap<>();
	private final AzertioRuntime runtime;
	private final UUID executionID;
	private UUID executionNodeID;
	private Benchmark benchmark;
	private ExecutionNodeStats lastBenchmarkStatistics;



	public ExecutionContext(AzertioRuntime runtime, UUID executionID, UUID executionNodeID) {
		this.runtime = runtime;
		this.executionID = executionID;
		this.executionNodeID = executionNodeID;
	}

	void setExecutionNodeID(UUID nodeID) {
		this.executionNodeID = nodeID;
	}

	public void setVariable(String name, String value) {
		variables.put(name, value);
	}

	public String getVariable(String name) {
		return variables.get(name);
	}


	public String interpolateString(String input) {
		for (Map.Entry<String, String> entry : variables.entrySet()) {
			String placeholder = "${" + entry.getKey() + "}";
			input = input.replace(placeholder, entry.getValue());
		}
		return input;
	}

	public void storeAttachment(byte[] bytes, String contentType) {
		TestExecutionRepository testExecutionRepository = runtime.getRepository(TestExecutionRepository.class);
		AttachmentRepository attachmentRepository = runtime.getRepository(AttachmentRepository.class);
		UUID attachmentID = testExecutionRepository.newAttachment(executionNodeID);
		attachmentRepository.storeAttachment(executionID, executionNodeID, attachmentID, bytes, contentType);
	}

	public void enableBenchmarkMode(Integer executions, Integer threads) {
		this.benchmark = new Benchmark(executions, threads, runtime.clock());
	}

	public void disableBenchmarkMode() {
		if (benchmark != null) {
			lastBenchmarkStatistics = benchmark.statistics();
		}
		this.benchmark = null;
	}

	public Benchmark benchmark() {
		return benchmark;
	}


	/**
	 * Runs the given task within the benchmark context, measuring its execution time and error status.
	 * If benchmark mode is not enabled, it simply executes the task without measuring.
	 * The task should return true if it succeeded, or false if it failed. Any thrown
	 * exceptions will be treated as failures and rethrown after recording the failure in the benchmark statistics.
	 * @param task the task to execute, returning true if it succeeded or false if it failed
	 * @throws Throwable if the task throws any exception, which will be rethrown
	 */
	public void runWithinBenchmark(BooleanSupplier task) {
		if (benchmark == null) {
			task.getAsBoolean();
			return;
		}
		int executionNumber = benchmark.markStarted();
		try {
			benchmark.markFinished(executionNumber, !task.getAsBoolean());
		} catch (Throwable t) {
			benchmark.markFinished(executionNumber, true);
			throw t;
		}
	}

	public boolean isBenchmarkMode() {
		return benchmark != null;
	}

	public ExecutionNodeStats lastBenchmarkStatistics() {
		return lastBenchmarkStatistics;
	}



}
