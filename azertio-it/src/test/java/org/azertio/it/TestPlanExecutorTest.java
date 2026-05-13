package org.azertio.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.execution.ExecutionResult;
import org.azertio.core.execution.TestExecution;
import org.azertio.core.execution.TestExecutionNode;
import org.azertio.core.execution.TestPlanExecutor;
import org.azertio.core.persistence.TestExecutionRepository;
import org.azertio.core.persistence.TestPlanNodeCriteria;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.*;
import org.azertio.persistence.execution.JooqExecutionRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestPlanExecutorTest {

	@Test
	void execute_passingStep_recordsPassedResult(@TempDir Path tempDir) {
		var ctx = setup("execPassingStep", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		UUID stepNodeID = findNodeOfType(ctx, NodeType.STEP);
		UUID execNodeID = ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), stepNodeID).orElseThrow();

		assertThat(ctx.execRepo().getExecutionNodeResult(execNodeID)).contains(ExecutionResult.PASSED);
		assertThat(ctx.execRepo().getExecutionNodeStartedAt(execNodeID)).isPresent();
		assertThat(ctx.execRepo().getExecutionNodeFinishedAt(execNodeID)).isPresent();
	}

	@Test
	void execute_failingStep_recordsFailedResultAndMessage(@TempDir Path tempDir) {
		var ctx = setup("execFailingStep", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		UUID stepNodeID = findNodeOfType(ctx, NodeType.STEP);
		UUID execNodeID = ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), stepNodeID).orElseThrow();

		assertThat(ctx.execRepo().getExecutionNodeResult(execNodeID)).contains(ExecutionResult.FAILED);
		assertThat(ctx.execRepo().getExecutionNodeMessage(execNodeID)).isPresent().get().asString().isNotBlank();
	}

	@Test
	void execute_errorStep_recordsErrorResultAndAttachment(@TempDir Path tempDir) {
		var ctx = setup("execErrorStep", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		UUID stepNodeID = findNodeOfType(ctx, NodeType.STEP);
		UUID execNodeID = ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), stepNodeID).orElseThrow();

		assertThat(ctx.execRepo().getExecutionNodeResult(execNodeID)).contains(ExecutionResult.ERROR);
		assertThat(ctx.execRepo().getExecutionNodeMessage(execNodeID)).isPresent().get().asString().isNotBlank();
	}

	@Test
	void execute_virtualStep_recordsPassedResultWithoutExecutingStep(@TempDir Path tempDir) {
		var ctx = setup("execVirtualStep", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		UUID virtualStepNodeID = findNodeOfType(ctx, NodeType.VIRTUAL_STEP);
		UUID execNodeID = ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), virtualStepNodeID).orElseThrow();

		assertThat(ctx.execRepo().getExecutionNodeResult(execNodeID)).contains(ExecutionResult.PASSED);
	}

	@Test
	void execute_allNodesHaveTimestamps(@TempDir Path tempDir) {
		var ctx = setup("execPassingStep", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		ctx.planRepo().getNodeDescendants(ctx.plan().planNodeRoot())
			.forEach(nodeID -> {
				UUID execNodeID = ctx.execRepo()
					.getExecutionNodeByPlanNode(execution.executionID(), nodeID)
					.orElseThrow(() -> new AssertionError("Missing execution node for plan node " + nodeID));
				assertThat(ctx.execRepo().getExecutionNodeStartedAt(execNodeID))
					.as("startedAt for node %s", nodeID).isPresent();
				assertThat(ctx.execRepo().getExecutionNodeFinishedAt(execNodeID))
					.as("finishedAt for node %s", nodeID).isPresent();
			});
	}

	@Test
	void execute_twoTestCases_allNodesRecorded(@TempDir Path tempDir) {
		var ctx = setup("execTwoTestCases", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		long testCaseCount = ctx.planRepo().getNodeDescendants(ctx.plan().planNodeRoot())
			.map(id -> ctx.planRepo().getNodeData(id).orElseThrow())
			.filter(n -> n.nodeType() == NodeType.TEST_CASE)
			.count();
		assertThat(testCaseCount).isEqualTo(2);

		ctx.planRepo().getNodeDescendants(ctx.plan().planNodeRoot())
			.forEach(nodeID -> assertThat(
				ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), nodeID)
			).as("execution node for plan node %s", nodeID).isPresent());
	}

	@Test
	void execute_twoTestCases_bothPass(@TempDir Path tempDir) {
		var ctx = setup("execTwoTestCases", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		ctx.planRepo().searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(ctx.plan().planNodeRoot()),
			TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE)
		)).forEach(testCaseNodeID -> {
			UUID execNodeID = ctx.execRepo()
				.getExecutionNodeByPlanNode(execution.executionID(), testCaseNodeID)
				.orElseThrow();
			assertThat(ctx.execRepo().getExecutionNodeResult(execNodeID))
				.as("result for test case %s", testCaseNodeID)
				.contains(ExecutionResult.PASSED);
		});
	}


	@Test
	void execute_mixedResults_setsTestCountsOnContainerNodes(@TempDir Path tempDir) {
		var ctx = setup("execMixedResults", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());
		UUID executionID = execution.executionID();

		// TEST_PLAN is the planNodeRoot itself (not a descendant), check it directly
		TestExecutionNode rootExecNode = ctx.execRepo()
			.getExecutionNode(executionID, ctx.plan().planNodeRoot()).orElseThrow();
		assertThat(rootExecNode.testPassedCount()).as("TEST_PLAN testPassedCount").isEqualTo(1);
		assertThat(rootExecNode.testFailedCount()).as("TEST_PLAN testFailedCount").isEqualTo(1);
		assertThat(rootExecNode.testErrorCount()).as("TEST_PLAN testErrorCount").isEqualTo(1);

		// TEST_SUITE and TEST_FEATURE are descendants — counts must also be 1/1/1
		for (NodeType containerType : List.of(NodeType.TEST_SUITE, NodeType.TEST_FEATURE)) {
			UUID planNodeID = findNodeOfType(ctx, containerType);
			TestExecutionNode execNode = ctx.execRepo().getExecutionNode(executionID, planNodeID).orElseThrow(
				() -> new AssertionError("Missing execution node for " + containerType)
			);
			assertThat(execNode.testPassedCount()).as("%s testPassedCount", containerType).isEqualTo(1);
			assertThat(execNode.testFailedCount()).as("%s testFailedCount", containerType).isEqualTo(1);
			assertThat(execNode.testErrorCount()).as("%s testErrorCount",  containerType).isEqualTo(1);
		}

		// TEST_CASE nodes must have null counts (they are the counted unit, not containers)
		ctx.planRepo().searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(ctx.plan().planNodeRoot()),
			TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE)
		)).forEach(testCaseNodeID -> {
			TestExecutionNode execNode = ctx.execRepo().getExecutionNode(executionID, testCaseNodeID).orElseThrow();
			assertThat(execNode.testPassedCount()).as("TEST_CASE testPassedCount must be null").isNull();
			assertThat(execNode.testFailedCount()).as("TEST_CASE testFailedCount must be null").isNull();
			assertThat(execNode.testErrorCount()).as("TEST_CASE testErrorCount must be null").isNull();
		});

		// EXECUTION row must also carry the aggregated counts
		TestExecution executionWithCounts = ctx.execRepo()
			.listExecutions(ctx.plan().planID(), ctx.plan().planNodeRoot(), 0, 1)
			.getFirst();
		assertThat(executionWithCounts.testPassedCount()).as("execution testPassedCount").isEqualTo(1);
		assertThat(executionWithCounts.testFailedCount()).as("execution testFailedCount").isEqualTo(1);
		assertThat(executionWithCounts.testErrorCount()).as("execution testErrorCount").isEqualTo(1);
	}


	@Test
	void execute_benchmarkMode_testCasePassesAndStatsStored(@TempDir Path tempDir) {
		var ctx = setup("execBenchmarkMode", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		UUID testCaseNodeID = findNodeOfType(ctx, NodeType.TEST_CASE);
		UUID testCaseExecNodeID = ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), testCaseNodeID).orElseThrow();
		assertThat(ctx.execRepo().getExecutionNodeResult(testCaseExecNodeID)).contains(ExecutionResult.PASSED);

		UUID benchStepNodeID = ctx.planRepo().searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(ctx.plan().planNodeRoot()),
			TestPlanNodeCriteria.withNodeType(NodeType.STEP)
		)).filter(id -> "a benchmarkable step".equals(
			ctx.planRepo().getNodeData(id).map(n -> n.name()).orElse("")
		)).findFirst().orElseThrow();

		UUID benchStepExecNodeID = ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), benchStepNodeID).orElseThrow();
		var stats = ctx.execRepo().getExecutionNodeStats(benchStepExecNodeID);
		assertThat(stats).isPresent();
		assertThat(stats.get().numExecutions()).isEqualTo(500);
		assertThat(stats.get().numThreads()).isEqualTo(10);
		assertThat(stats.get().errorRate()).isEqualTo(0.0);
		assertThat(stats.get().min()).isGreaterThanOrEqualTo(0);
		assertThat(stats.get().max()).isGreaterThanOrEqualTo(stats.get().min());
	}

	// ---- helpers ----

	private record ExecutionContext(
		AzertioRuntime runtime,
		TestPlan plan,
		TestPlanRepository planRepo,
		JooqExecutionRepository execRepo
	) {}

	private ExecutionContext setup(String suiteName, Path tempDir) {
		Config config = Config.ofMap(Map.of(
			AzertioConfig.ENV_PATH,          tempDir.toString(),
			AzertioConfig.PERSISTENCE_MODE,  AzertioConfig.PERSISTENCE_MODE_FILE,
			AzertioConfig.PERSISTENCE_FILE,  tempDir.resolve("test.db").toString()
		));
		AzertioRuntime runtime = new AzertioRuntime(config);
		TestPlan plan = runtime.buildTestPlan(createContext(suiteName, config));
		TestPlanRepository planRepo = runtime.getRepository(TestPlanRepository.class);
		JooqExecutionRepository execRepo = (JooqExecutionRepository) runtime.getRepository(TestExecutionRepository.class);
		return new ExecutionContext(runtime, plan, planRepo, execRepo);
	}

	private AzertioContext createContext(String suiteName, Config config) {
		TestSuite suite = new TestSuite(suiteName, "", null);
		TestProject project = new TestProject("Test Project", "", "Test Org", List.of(suite));
		return new AzertioContext(project, config, List.of(suiteName), List.of());
	}

	private UUID findNodeOfType(ExecutionContext ctx, NodeType type) {
		return ctx.planRepo().searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(ctx.plan().planNodeRoot()),
			TestPlanNodeCriteria.withNodeType(type)
		)).findFirst().orElseThrow(() -> new AssertionError("No node of type " + type + " found"));
	}

}
