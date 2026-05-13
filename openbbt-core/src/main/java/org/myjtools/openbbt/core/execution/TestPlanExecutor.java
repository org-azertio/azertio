package org.myjtools.openbbt.core.execution;

import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.backend.Benchmark;
import org.myjtools.openbbt.core.contributors.StatisticsProvider;
import org.myjtools.openbbt.core.events.ExecutionFinished;
import org.myjtools.openbbt.core.events.ExecutionNodeFinished;
import org.myjtools.openbbt.core.events.ExecutionNodeStarted;
import org.myjtools.openbbt.core.events.ExecutionStarted;
import org.myjtools.openbbt.core.persistence.AttachmentRepository;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.NodeType;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import org.myjtools.openbbt.core.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TestPlanExecutor {

	private record Result(ExecutionResult result, String message, Throwable error) {}

	private record NodeResult(ExecutionResult result, int passedCount, int errorCount, int failedCount) {
		static final NodeResult PASSED_LEAF = new NodeResult(ExecutionResult.PASSED, 0, 0, 0);
		static NodeResult ofTestCase(ExecutionResult result) {
			return new NodeResult(
				result,
				result == ExecutionResult.PASSED ? 1 : 0,
				(result == ExecutionResult.ERROR || result == ExecutionResult.UNDEFINED) ? 1 : 0,
				result == ExecutionResult.FAILED ? 1 : 0
			);
		}
		NodeResult merge(NodeResult other) {
			return new NodeResult(
				TestPlanExecutor.merge(result, other.result),
				passedCount + other.passedCount,
				errorCount + other.errorCount,
				failedCount + other.failedCount
			);
		}
	}


	private static final Log log = Log.of();

	private final OpenBBTRuntime runtime;
	private final TestPlanRepository testPlanRepository;
	private final TestExecutionRepository testExecutionRepository;
	private final AttachmentRepository attachmentRepository;
	private final String parallelTag;
	private final ExecutorService parallelExecutor = Executors.newCachedThreadPool();

	public TestPlanExecutor(OpenBBTRuntime runtime) {
		this.runtime = runtime;
		this.testPlanRepository = runtime.getRepository(TestPlanRepository.class);
		this.testExecutionRepository = runtime.getRepository(TestExecutionRepository.class);
		this.attachmentRepository = runtime.getRepository(AttachmentRepository.class);
		this.parallelTag = runtime.configuration().getString(OpenBBTConfig.PARALLEL_EXECUTION_TAG).orElseThrow(
			() -> new OpenBBTException("Configuration key {} not found", OpenBBTConfig.PARALLEL_EXECUTION_TAG)
		);
	}

	public TestExecution execute(UUID planID) {
		return execute(planID, null);
	}

	public TestExecution execute(UUID planID, Consumer<UUID> onExecutionCreated) {
		TestPlan testPlan = testPlanRepository.getPlan(planID).orElseThrow(
			() -> new OpenBBTException("Test plan with ID {} not found", planID)
		);
		TestPlanNode planRoot = testPlanRepository.getNodeData(testPlan.planNodeRoot()).orElseThrow(
			() -> new OpenBBTException("Test plan root node with ID {} not found", testPlan.planNodeRoot())
		);
		if (planRoot.hasIssues()) {
			throw new OpenBBTException("Test plan has issues, cannot be executed");
		}
		String profileName = runtime.profile().name().isBlank() ? null : runtime.profile().name();
		TestExecution execution = testExecutionRepository.newExecution(planID, runtime.clock().now(), profileName);
		if (onExecutionCreated != null) {
			onExecutionCreated.accept(execution.executionID());
		}
		UUID rootExecutionNodeID = createExecutionNodes(execution.executionID(), planRoot.nodeID());
		execution.executionRootNodeID(rootExecutionNodeID);
		runtime.eventBus().publish(
			new ExecutionStarted(runtime.clock().now(), execution.executionID(), planID, profileName)
		);

		NodeResult rootResult = executeTestPlanNode(execution.executionID(), planRoot.nodeID(), null);
		testExecutionRepository.updateExecutionTestCounts(
			execution.executionID(), rootResult.passedCount(), rootResult.errorCount(), rootResult.failedCount()
		);
		runtime.eventBus().publish(
			new ExecutionFinished(runtime.clock().now(), execution.executionID(), planID, profileName, rootResult.result)
		);
		return execution;
	}


	private NodeResult executeTestPlanNode(UUID executionID, UUID testPlanNodeID, BackendExecutor backendExecutor) {
		UUID executionNodeID = testExecutionRepository.getExecutionNodeByPlanNode(executionID, testPlanNodeID)
		.orElseThrow(
			() -> new OpenBBTException("Execution node for test plan node with ID {} not found", testPlanNodeID)
		);
		Instant start = runtime.clock().now();
		testExecutionRepository.updateExecutionNodeStart(executionNodeID, start);
		runtime.eventBus().publish(
			new ExecutionNodeStarted(start, executionID, executionNodeID, testPlanNodeID)
		);
		try {
			NodeResult nodeResult = doExecuteTestPlanNode(executionID, executionNodeID, testPlanNodeID, backendExecutor);
			Instant finish = runtime.clock().now();
			testExecutionRepository.updateExecutionNodeFinish(executionNodeID, nodeResult.result(), finish);
			runtime.eventBus().publish(
				new ExecutionNodeFinished(finish, executionID, executionNodeID, testPlanNodeID, nodeResult.result())
			);
			return nodeResult;
		} catch (Exception e) {
			log.error("Unexpected error executing plan node {}: {}", testPlanNodeID, e.getMessage());
			log.error(e);
			Instant finish = runtime.clock().now();
			testExecutionRepository.updateExecutionNodeFinish(executionNodeID, ExecutionResult.ERROR, finish);
			storeStackTraceAttachment(executionID, executionNodeID, e);
			runtime.eventBus().publish(
				new ExecutionNodeFinished(finish, executionID, executionNodeID, testPlanNodeID, ExecutionResult.ERROR)
			);
			return new NodeResult(ExecutionResult.ERROR, 0, 0, 0);
		}
	}


	private NodeResult doExecuteTestPlanNode(
		UUID executionID,
		UUID executionNodeID,
		UUID testPlanNodeID,
		BackendExecutor backendExecutor
	) {
		TestPlanNode node = testPlanRepository.getNodeData(testPlanNodeID).orElseThrow(
			() -> new OpenBBTException("Test plan node with ID {} not found", testPlanNodeID)
		);

		if (node.nodeType() == NodeType.VIRTUAL_STEP) {
			return NodeResult.PASSED_LEAF;
		}

		ExecutionResult ownResult = ExecutionResult.PASSED;
		if (node.nodeType() == NodeType.STEP) {
			ownResult = recordStepExecution(executionID, executionNodeID, backendExecutor, node);
		} else if (node.nodeType() == NodeType.TEST_CASE) {
			backendExecutor = new BackendExecutor(runtime);
			backendExecutor.setUp(executionID, executionNodeID, node.properties());
		}

		NodeResult childrenResult = executeChildren(executionID, testPlanNodeID, backendExecutor);

		if (node.nodeType() == NodeType.TEST_CASE) {
			backendExecutor.tearDown();
		}

		ExecutionResult mergedResult = merge(ownResult, childrenResult.result());

		if (node.nodeType() == NodeType.TEST_CASE) {
			return NodeResult.ofTestCase(mergedResult);
		}

		NodeResult nodeResult = new NodeResult(mergedResult, childrenResult.passedCount(), childrenResult.errorCount(), childrenResult.failedCount());
		NodeType nodeType = node.nodeType();
		if (nodeType == NodeType.TEST_PLAN || nodeType == NodeType.TEST_SUITE || nodeType == NodeType.TEST_FEATURE) {
			testExecutionRepository.updateExecutionNodeTestCounts(
				executionNodeID, nodeResult.passedCount(), nodeResult.errorCount(), nodeResult.failedCount()
			);
		}
		return nodeResult;
	}


	private NodeResult executeChildren(UUID executionID, UUID testPlanNodeID, BackendExecutor backendExecutor) {
		List<UUID> children = testPlanRepository.getNodeChildren(testPlanNodeID).toList();
		if (children.isEmpty()) {
			return NodeResult.PASSED_LEAF;
		}
		if (backendExecutor == null && children.size() > 1) {
			return executeChildrenParallel(executionID, children);
		}
		return executeChildrenSequential(executionID, children, backendExecutor);
	}


	private NodeResult executeChildrenParallel(UUID executionID, List<UUID> children) {
		List<CompletableFuture<NodeResult>> parallelFutures = new ArrayList<>();
		NodeResult result = NodeResult.PASSED_LEAF;
		for (UUID childNodeID : children) {
			TestPlanNode childNode = testPlanRepository.getNodeData(childNodeID).orElseThrow(
				() -> new OpenBBTException("Test plan node with ID {} not found", childNodeID)
			);
			if (childNode.hasTag(parallelTag)) {
				parallelFutures.add(CompletableFuture.supplyAsync(
					() -> executeTestPlanNode(executionID, childNodeID, null),
					parallelExecutor
				));
			} else {
				result = result.merge(executeTestPlanNode(executionID, childNodeID, null));
			}
		}
		for (CompletableFuture<NodeResult> future : parallelFutures) {
			result = result.merge(future.join());
		}
		return result;
	}


	private NodeResult executeChildrenSequential(UUID executionID, List<UUID> children, BackendExecutor backendExecutor) {
		NodeResult finalResult = NodeResult.PASSED_LEAF;
		for (UUID childNodeID : children) {
			finalResult = finalResult.merge(executeTestPlanNode(executionID, childNodeID, backendExecutor));
		}
		return finalResult;
	}


	private ExecutionResult recordStepExecution(
		UUID executionID, UUID executionNodeID, BackendExecutor backendExecutor, TestPlanNode node
	) {
		Benchmark benchmark = backendExecutor.currentBenchmark();
		Result stepResult = executeTestCaseStep(backendExecutor, node, executionNodeID, benchmark);
		if (stepResult.message() != null) {
			testExecutionRepository.updateExecutionNodeMessage(executionNodeID, stepResult.message());
		}
		if (stepResult.error() != null) {
			storeStackTraceAttachment(executionID, executionNodeID, stepResult.error());
		}
		if (benchmark != null) {
			testExecutionRepository.storeExecutionNodeStats(executionNodeID, benchmark.statistics());
			backendExecutor.disableBenchmarkMode();
		}
		return stepResult.result();
	}


	private Result executeTestCaseStep(
		BackendExecutor backendExecutor,
		TestPlanNode node,
		UUID executionNodeID,
		Benchmark benchmark
	) {
		if (benchmark != null) {
			try {
				if (!backendExecutor.hasAnnotation(node, StatisticsProvider.class)) {
					throw new OpenBBTException(
						"Step '{}' cannot be executed in benchmark mode because it does not provide statistics, which are required in benchmark mode.",
						node.name()
					);
				}
				backendExecutor.executeBenchmark(node, executionNodeID, benchmark);
				return new Result(ExecutionResult.PASSED, null, null);
			} catch (Exception e) {
				log.error(e);
				return new Result(ExecutionResult.ERROR, e.getMessage(), e);
			}
		}

		try {
			long timeoutSec = Long.parseLong(node.properties().getOrDefault(OpenBBTConfig.STEP_EXECUTION_TIMEOUT, "-1"));
			if (timeoutSec == -1L) {
				timeoutSec = runtime.configuration().getLong(OpenBBTConfig.STEP_EXECUTION_TIMEOUT).orElse(-1L);
			}
			var stepResult = backendExecutor.submitStepExecution(node, executionNodeID, timeoutSec).get();
			if (stepResult.left() == ExecutionResult.PASSED) {
				return new Result(ExecutionResult.PASSED,null,null);
			} else if (stepResult.left() == ExecutionResult.FAILED) {
				return new Result(ExecutionResult.FAILED, stepResult.right().getMessage(), null);
			} else if (stepResult.left() == ExecutionResult.UNDEFINED) {
				log.error("Step execution failed with no matching step definition: {}", stepResult.right().getMessage());
				return new Result(ExecutionResult.UNDEFINED, node.name(), null);
			} else if (stepResult.left() == ExecutionResult.SKIPPED) {
				return new Result(ExecutionResult.SKIPPED, null, null);
			}else {
				return new Result(ExecutionResult.ERROR, stepResult.right().getMessage(), stepResult.right());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error(e);
			return new Result(ExecutionResult.ERROR, e.getMessage(), e);
		} catch (Exception e) {
			log.error(e);
			return new Result(ExecutionResult.ERROR, e.getMessage(), e);
		}
	}


	private UUID createExecutionNodes(UUID executionID, UUID planNodeID) {
		UUID executionNodeID = testExecutionRepository.newExecutionNode(executionID, planNodeID);
		testPlanRepository.getNodeChildren(planNodeID)
			.forEach(childNodeID -> createExecutionNodes(executionID, childNodeID));
		return executionNodeID;
	}


	private void storeStackTraceAttachment(UUID executionID, UUID executionNodeID, Throwable error) {
		UUID attachmentID = testExecutionRepository.newAttachment(executionNodeID);
		String stackTrace = getStackTraceAsString(error);
		attachmentRepository.storeAttachment(
			executionID,
			executionNodeID,
			attachmentID,
			stackTrace.getBytes(StandardCharsets.UTF_8),
			"text/plain"
		);
	}


	private String getStackTraceAsString(Throwable error) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		error.printStackTrace(pw);
		return sw.toString();
	}


	private static ExecutionResult merge(ExecutionResult a, ExecutionResult b) {
		return mergePriority(a) >= mergePriority(b) ? a : b;
	}

	private static int mergePriority(ExecutionResult result) {
		return switch (result) {
			case ERROR    -> 4;
			case FAILED   -> 3;
			case SKIPPED  -> 2;
			case PASSED   -> 1;
			default       -> 4; // UNDEFINED treated as ERROR
		};
	}

}