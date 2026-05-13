package org.azertio.core.execution;

import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioException;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.backend.Benchmark;
import org.azertio.core.backend.ExecutionContext;
import org.azertio.core.backend.StepProviderBackend;
import org.azertio.core.testplan.NodeArgument;
import org.azertio.core.testplan.TestPlanNode;
import org.azertio.core.util.Log;
import org.azertio.core.util.Pair;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class BackendExecutor {

	private static final Log log = Log.of();
	private static final long NO_TIMEOUT = Long.MAX_VALUE;

	private final StepProviderBackend backend;
	private final ExecutorService executor;
	private final long stepTimeoutSec;

	private boolean testCaseFailed = false;


	public BackendExecutor(AzertioRuntime runtime) {
		this.backend = new StepProviderBackend(runtime);
		this.executor = Executors.newSingleThreadExecutor(); // TODO: make this configurable for parallel execution in the future
		this.stepTimeoutSec = runtime.configuration().getLong(AzertioConfig.STEP_EXECUTION_TIMEOUT).orElse(NO_TIMEOUT);
	}

	public void setUp(UUID executionID, UUID executionNodeID, Map<String, String> properties) {
		runInExecutor(() -> backend.setUp(executionID, executionNodeID, properties));
	}

	public void tearDown() {
		runInExecutor(backend::tearDown);
	}

	private void runInExecutor(Runnable task) {
		try {
			executor.submit(task).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AzertioException(e, "Interrupted while running task in executor");
		} catch (ExecutionException e) {
			throw new AzertioException(e.getCause(), "Task failed in executor");
		}
	}


	public Future<Pair<ExecutionResult, Throwable>> submitStepExecution(TestPlanNode node) {
		return submitStepExecution(node, null, NO_TIMEOUT);
	}


	public Future<Pair<ExecutionResult, Throwable>> submitStepExecution(
			TestPlanNode node,
			UUID executionNodeID,
			long timeoutSec
	) {
		if (timeoutSec == 0 || timeoutSec == -1) {
			throw new AzertioException(
					"Invalid timeout value: {}. Step timeout must be a positive number of seconds.", timeoutSec
			);
		}
		Future<Pair<ExecutionResult, Throwable>> future = this.executor.submit(() -> {
			try {
				if (testCaseFailed) {
					return Pair.of(ExecutionResult.SKIPPED, null);
				}
				backend.run(node.name(), locale(node.language()), nodeArgument(node), executionNodeID);
				return Pair.of(ExecutionResult.PASSED, null);
			} catch (AssertionError e) {
				testCaseFailed = true;
				return Pair.of(ExecutionResult.FAILED, e);
			} catch (NoMatchingStepException e) {
				testCaseFailed = true;
				return Pair.of(ExecutionResult.UNDEFINED, e);
			} catch (Exception e) {
				testCaseFailed = true;
				log.error(e);
				return Pair.of(ExecutionResult.ERROR, e);
			}
		});
		try {
			Pair<ExecutionResult, Throwable> result = timeoutSec == NO_TIMEOUT
					? future.get()
					: future.get(timeoutSec, TimeUnit.SECONDS);
			return CompletableFuture.completedFuture(result);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw new AzertioException("Step execution timed out after {} seconds: {}", timeoutSec, node.name());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AzertioException(e, "Interrupted while waiting for step execution");
		} catch (ExecutionException e) {
			throw new AzertioException(e.getCause(), "Unexpected error in step execution");
		}
	}


	public Benchmark currentBenchmark() {
		try {
			return executor.submit(() -> {
				ExecutionContext ctx = ExecutionContext.current();
				return ctx != null ? ctx.benchmark() : null;
			}).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (ExecutionException e) {
			return null;
		}
	}

	public void executeBenchmark(TestPlanNode node, UUID executionNodeID, Benchmark benchmark) {
		runInExecutor(() -> runBenchmarkIterations(node, executionNodeID, benchmark));
	}

	public void disableBenchmarkMode() {
		runInExecutor(() -> {
			ExecutionContext ctx = ExecutionContext.current();
			if (ctx != null) ctx.disableBenchmarkMode();
		});
	}

	private void runBenchmarkIterations(TestPlanNode node, UUID executionNodeID, Benchmark benchmark) {
		var semaphore = new Semaphore(benchmark.numThreads());
		var futures = new ArrayList<Future<?>>(benchmark.totalExecutions());
		try (var virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
			for (int i = 0; i < benchmark.totalExecutions(); i++) {
				futures.add(virtualExecutor.submit(ExecutionContext.withCurrent(() -> {
					semaphore.acquireUninterruptibly();
					try {
						backend.run(node.name(), locale(node.language()), nodeArgument(node), executionNodeID);
					} catch (Throwable ignored) {
						// errors are tracked by the step itself via benchmark.markFinished
					} finally {
						semaphore.release();
					}
				})));
			}
			for (var future : futures) {
				try {
					future.get(stepTimeoutSec, TimeUnit.SECONDS);
				} catch (TimeoutException e) {
					future.cancel(true);
					log.error("Benchmark iteration timed out after {} seconds", stepTimeoutSec);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new AzertioException(e, "Interrupted during benchmark execution");
				} catch (ExecutionException e) {
					log.error(e.getCause(), "Unexpected error in benchmark iteration");
				}
			}
		}
	}

	private NodeArgument nodeArgument(TestPlanNode node) {
		NodeArgument nodeArgument = null;
		if (node.document() != null) {
			nodeArgument = node.document();
		} else if (node.dataTable() != null) {
			nodeArgument = node.dataTable();
		}
		return nodeArgument;
	}


	private Locale locale(String language) {
		if (language == null || language.isBlank()) {
			return Locale.ENGLISH;
		}
		return Locale.forLanguageTag(language);
	}


	public boolean hasAnnotation(TestPlanNode node, Class<? extends Annotation> annotationClass) {
		var matchingStep = backend.matchingStep(node.name(), locale(node.language()));
		return matchingStep.map(Pair::left).map(stepMethod -> stepMethod.hasAnnotation(annotationClass)).orElse(false);
	}

}
