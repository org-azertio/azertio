package org.myjtools.openbbt.persistence.test.execution;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.execution.ExecutionNodeStats;
import org.myjtools.openbbt.core.execution.ExecutionResult;
import org.myjtools.openbbt.core.execution.TestExecution;
import org.myjtools.openbbt.core.testplan.NodeType;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import org.myjtools.openbbt.core.testplan.TestProject;
import org.myjtools.openbbt.persistence.DataSourceProvider;
import org.myjtools.openbbt.persistence.execution.JooqExecutionRepository;
import org.myjtools.openbbt.persistence.plan.JooqPlanRepository;
import javax.sql.DataSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractExecutionRepositoryTest {

	protected JooqExecutionRepository repo;
	protected JooqPlanRepository planRepo;
	private DataSource dataSource;

	protected abstract DataSourceProvider dataSourceProvider();

	@BeforeEach
	void setUp() {
		DataSourceProvider provider = dataSourceProvider();
		dataSource = provider.obtainDataSource();
		planRepo = new JooqPlanRepository(dataSource, provider.dialect());
		repo = new JooqExecutionRepository(dataSource, provider.dialect());
		repo.clearAllData();
		planRepo.clearAllData();
	}

	@AfterEach
	void tearDown() {
		if (dataSource instanceof HikariDataSource hikari) {
			hikari.close();
		}
	}

	// --- helpers ---

	private UUID persistPlanWithRoot() {
		UUID root = planRepo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID projectID = planRepo.persistProject(new TestProject("P", "desc", "Org", List.of()));
		TestPlan plan = planRepo.persistPlan(new TestPlan(null, projectID, Instant.now(), "rh", "ch", root, 0, null));
		return plan.planID();
	}

	private UUID persistPlanNodeUnder(UUID parent, NodeType type, String name) {
		UUID node = planRepo.persistNode(new TestPlanNode().nodeType(type).name(name));
		planRepo.attachChildNodeLast(parent, node);
		return node;
	}

	// --- newExecution ---

	@Test
	void newExecution_returnsPopulatedExecution() {
		UUID planID = persistPlanWithRoot();
		Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		TestExecution execution = repo.newExecution(planID, now, null);

		assertThat(execution.executionID()).isNotNull();
		assertThat(execution.planID()).isEqualTo(planID);
		assertThat(execution.executedAt()).isEqualTo(now);
	}

	@Test
	void newExecution_eachCallProducesDistinctId() {
		UUID planID = persistPlanWithRoot();

		TestExecution first = repo.newExecution(planID, Instant.now(), null);
		TestExecution second = repo.newExecution(planID, Instant.now(), null);

		assertThat(first.executionID()).isNotEqualTo(second.executionID());
	}

	// --- newExecutionNode ---

	@Test
	void newExecutionNode_returnsNewId() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);

		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		assertThat(executionNodeID).isNotNull();
	}

	@Test
	void newExecutionNode_multipleNodesForSameExecution_returnDistinctIds() {
		UUID planID = persistPlanWithRoot();
		UUID rootPlanNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		UUID casePlanNodeID = persistPlanNodeUnder(rootPlanNodeID, NodeType.TEST_CASE, "case");
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);

		UUID node1 = repo.newExecutionNode(execution.executionID(), rootPlanNodeID);
		UUID node2 = repo.newExecutionNode(execution.executionID(), casePlanNodeID);

		assertThat(node1).isNotEqualTo(node2);
	}

	// --- getExecutionNodeByPlanNode ---

	@Test
	void getExecutionNodeByPlanNode_returnsCorrectNode() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		Optional<UUID> found = repo.getExecutionNodeByPlanNode(execution.executionID(), planNodeID);

		assertThat(found).contains(executionNodeID);
	}

	@Test
	void getExecutionNodeByPlanNode_returnsEmptyForUnknownPlanNode() {
		UUID planID = persistPlanWithRoot();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);

		Optional<UUID> found = repo.getExecutionNodeByPlanNode(execution.executionID(), UUID.randomUUID());

		assertThat(found).isEmpty();
	}

	@Test
	void getExecutionNodeByPlanNode_isolatesAcrossExecutions() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution exec1 = repo.newExecution(planID, Instant.now(), null);
		TestExecution exec2 = repo.newExecution(planID, Instant.now(), null);
		UUID nodeInExec1 = repo.newExecutionNode(exec1.executionID(), planNodeID);
		UUID nodeInExec2 = repo.newExecutionNode(exec2.executionID(), planNodeID);

		assertThat(repo.getExecutionNodeByPlanNode(exec1.executionID(), planNodeID)).contains(nodeInExec1);
		assertThat(repo.getExecutionNodeByPlanNode(exec2.executionID(), planNodeID)).contains(nodeInExec2);
	}

	// --- updateExecutionNodeStart ---

	@Test
	void updateExecutionNodeStart_setsStartedAt() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);
		Instant startedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		repo.updateExecutionNodeStart(executionNodeID, startedAt);

		assertThat(repo.getExecutionNodeStartedAt(executionNodeID)).contains(startedAt);
	}

	@Test
	void updateExecutionNodeStart_isNullBeforeUpdate() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		assertThat(repo.getExecutionNodeStartedAt(executionNodeID)).isEmpty();
	}

	// --- updateExecutionNodeFinish ---

	@Test
	void updateExecutionNodeFinish_setsResultAndFinishedAt() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);
		Instant finishedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		repo.updateExecutionNodeFinish(executionNodeID, ExecutionResult.PASSED, finishedAt);

		assertThat(repo.getExecutionNodeResult(executionNodeID)).contains(ExecutionResult.PASSED);
		assertThat(repo.getExecutionNodeFinishedAt(executionNodeID)).contains(finishedAt);
	}

	@Test
	void updateExecutionNodeFinish_allResultValues() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);

		for (ExecutionResult result : ExecutionResult.values()) {
			UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);
			repo.updateExecutionNodeFinish(executionNodeID, result, Instant.now());
			assertThat(repo.getExecutionNodeResult(executionNodeID)).contains(result);
		}
	}

	@Test
	void updateExecutionNodeFinish_resultIsNullBeforeUpdate() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		assertThat(repo.getExecutionNodeResult(executionNodeID)).isEmpty();
		assertThat(repo.getExecutionNodeFinishedAt(executionNodeID)).isEmpty();
	}

	// --- updateExecutionNodeMessage ---

	@Test
	void updateExecutionNodeMessage_setsMessage() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		repo.updateExecutionNodeMessage(executionNodeID, "assertion failed: expected 200 but was 404");

		assertThat(repo.getExecutionNodeMessage(executionNodeID))
			.contains("assertion failed: expected 200 but was 404");
	}

	@Test
	void updateExecutionNodeMessage_isNullBeforeUpdate() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		assertThat(repo.getExecutionNodeMessage(executionNodeID)).isEmpty();
	}

	@Test
	void updateExecutionNodeMessage_overwritesPreviousMessage() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		repo.updateExecutionNodeMessage(executionNodeID, "first message");
		repo.updateExecutionNodeMessage(executionNodeID, "second message");

		assertThat(repo.getExecutionNodeMessage(executionNodeID)).contains("second message");
	}

	// --- newAttachment ---

	@Test
	void newAttachment_createsAttachmentRecord() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		UUID attachmentID = repo.newAttachment(executionNodeID);

		assertThat(attachmentID).isNotNull();
		assertThat(repo.existsAttachment(attachmentID)).isTrue();
	}

	@Test
	void newAttachment_multipleAttachmentsForSameNode_returnDistinctIds() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		UUID att1 = repo.newAttachment(executionNodeID);
		UUID att2 = repo.newAttachment(executionNodeID);

		assertThat(att1).isNotEqualTo(att2);
		assertThat(repo.existsAttachment(att1)).isTrue();
		assertThat(repo.existsAttachment(att2)).isTrue();
	}

	// --- listExecutions ---

	private UUID rootPlanNodeOf(UUID planID) {
		return planRepo.getPlan(planID).orElseThrow().planNodeRoot();
	}

	private TestExecution executionWithRootNode(UUID planID, UUID planNodeRoot, Instant executedAt, ExecutionResult result) {
		TestExecution ex = repo.newExecution(planID, executedAt, null);
		UUID rootExecNodeID = repo.newExecutionNode(ex.executionID(), planNodeRoot);
		repo.updateExecutionNodeFinish(rootExecNodeID, result, executedAt.plusSeconds(1));
		return ex;
	}

	@Test
	void listExecutions_returnsExecutionsForPlan() {
		UUID planID = persistPlanWithRoot();
		UUID root = rootPlanNodeOf(planID);
		executionWithRootNode(planID, root, Instant.now().minusSeconds(10), ExecutionResult.PASSED);
		executionWithRootNode(planID, root, Instant.now(), ExecutionResult.FAILED);

		assertThat(repo.listExecutions(planID, root, 0, 0)).hasSize(2);
	}

	@Test
	void listExecutions_returnsEmptyForUnknownPlan() {
		UUID root = UUID.randomUUID();
		assertThat(repo.listExecutions(UUID.randomUUID(), root, 0, 0)).isEmpty();
	}

	@Test
	void listExecutions_excludesExecutionsFromOtherPlans() {
		UUID planA = persistPlanWithRoot();
		UUID planB = persistPlanWithRoot();
		UUID rootA = rootPlanNodeOf(planA);
		UUID rootB = rootPlanNodeOf(planB);
		executionWithRootNode(planA, rootA, Instant.now(), ExecutionResult.PASSED);
		executionWithRootNode(planB, rootB, Instant.now(), ExecutionResult.FAILED);

		List<TestExecution> result = repo.listExecutions(planA, rootA, 0, 0);
		assertThat(result).hasSize(1);
		assertThat(result.get(0).planID()).isEqualTo(planA);
	}

	@Test
	void listExecutions_orderedByExecutedAtDescending() {
		UUID planID = persistPlanWithRoot();
		UUID root = rootPlanNodeOf(planID);
		Instant t1 = Instant.now().minusSeconds(200).truncatedTo(ChronoUnit.MILLIS);
		Instant t2 = Instant.now().minusSeconds(100).truncatedTo(ChronoUnit.MILLIS);
		Instant t3 = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		TestExecution e1 = executionWithRootNode(planID, root, t1, ExecutionResult.PASSED);
		TestExecution e2 = executionWithRootNode(planID, root, t2, ExecutionResult.FAILED);
		TestExecution e3 = executionWithRootNode(planID, root, t3, ExecutionResult.ERROR);

		List<TestExecution> result = repo.listExecutions(planID, root, 0, 0);
		assertThat(result).hasSize(3);
		assertThat(result.get(0).executionID()).isEqualTo(e3.executionID());
		assertThat(result.get(1).executionID()).isEqualTo(e2.executionID());
		assertThat(result.get(2).executionID()).isEqualTo(e1.executionID());
	}

	@Test
	void listExecutions_populatesExecutionRootNodeID() {
		UUID planID = persistPlanWithRoot();
		UUID rootNodeID = rootPlanNodeOf(planID);
		TestExecution ex = repo.newExecution(planID, Instant.now(), null);
		UUID rootExecNodeID = repo.newExecutionNode(ex.executionID(), rootNodeID);

		List<TestExecution> result = repo.listExecutions(planID, rootNodeID, 0, 0);
		assertThat(result).hasSize(1);
		assertThat(result.get(0).executionRootNodeID()).isEqualTo(rootExecNodeID);
	}

	@Test
	void listExecutions_executionRootNodeIdIsNullWhenNoExecutionNodes() {
		UUID planID = persistPlanWithRoot();
		UUID rootNodeID = rootPlanNodeOf(planID);
		repo.newExecution(planID, Instant.now(), null);

		List<TestExecution> result = repo.listExecutions(planID, rootNodeID, 0, 0);
		assertThat(result).hasSize(1);
		assertThat(result.get(0).executionRootNodeID()).isNull();
	}

	@Test
	void listExecutions_withMax_limitsResults() {
		UUID planID = persistPlanWithRoot();
		UUID root = rootPlanNodeOf(planID);
		executionWithRootNode(planID, root, Instant.now().minusSeconds(200), ExecutionResult.PASSED);
		executionWithRootNode(planID, root, Instant.now().minusSeconds(100), ExecutionResult.FAILED);
		executionWithRootNode(planID, root, Instant.now(), ExecutionResult.ERROR);

		assertThat(repo.listExecutions(planID, root, 0, 2)).hasSize(2);
	}

	@Test
	void listExecutions_withMaxZero_returnsAll() {
		UUID planID = persistPlanWithRoot();
		UUID root = rootPlanNodeOf(planID);
		executionWithRootNode(planID, root, Instant.now().minusSeconds(200), ExecutionResult.PASSED);
		executionWithRootNode(planID, root, Instant.now().minusSeconds(100), ExecutionResult.FAILED);
		executionWithRootNode(planID, root, Instant.now(), ExecutionResult.ERROR);

		assertThat(repo.listExecutions(planID, root, 0, 0)).hasSize(3);
	}

	@Test
	void listExecutions_withOffset_skipsRecords() {
		UUID planID = persistPlanWithRoot();
		UUID root = rootPlanNodeOf(planID);
		Instant t1 = Instant.now().minusSeconds(200).truncatedTo(ChronoUnit.MILLIS);
		Instant t2 = Instant.now().minusSeconds(100).truncatedTo(ChronoUnit.MILLIS);
		Instant t3 = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		TestExecution e1 = executionWithRootNode(planID, root, t1, ExecutionResult.PASSED);
		TestExecution e2 = executionWithRootNode(planID, root, t2, ExecutionResult.FAILED);
		executionWithRootNode(planID, root, t3, ExecutionResult.ERROR);

		// Desc order: [e3, e2, e1]. Offset=1 skips e3 -> [e2, e1]
		List<TestExecution> result = repo.listExecutions(planID, root, 1, 0);
		assertThat(result).hasSize(2);
		assertThat(result.get(0).executionID()).isEqualTo(e2.executionID());
		assertThat(result.get(1).executionID()).isEqualTo(e1.executionID());
	}

	@Test
	void listExecutions_withOffsetAndMax_paginates() {
		UUID planID = persistPlanWithRoot();
		UUID root = rootPlanNodeOf(planID);
		Instant t1 = Instant.now().minusSeconds(300).truncatedTo(ChronoUnit.MILLIS);
		Instant t2 = Instant.now().minusSeconds(200).truncatedTo(ChronoUnit.MILLIS);
		Instant t3 = Instant.now().minusSeconds(100).truncatedTo(ChronoUnit.MILLIS);
		Instant t4 = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		TestExecution e1 = executionWithRootNode(planID, root, t1, ExecutionResult.PASSED);
		TestExecution e2 = executionWithRootNode(planID, root, t2, ExecutionResult.FAILED);
		TestExecution e3 = executionWithRootNode(planID, root, t3, ExecutionResult.ERROR);
		TestExecution e4 = executionWithRootNode(planID, root, t4, ExecutionResult.SKIPPED);

		// Desc: [e4, e3, e2, e1]. Page 1: offset=0, max=2 -> [e4, e3]
		List<TestExecution> page1 = repo.listExecutions(planID, root, 0, 2);
		assertThat(page1.get(0).executionID()).isEqualTo(e4.executionID());
		assertThat(page1.get(1).executionID()).isEqualTo(e3.executionID());

		// Page 2: offset=2, max=2 -> [e2, e1]
		List<TestExecution> page2 = repo.listExecutions(planID, root, 2, 2);
		assertThat(page2.get(0).executionID()).isEqualTo(e2.executionID());
		assertThat(page2.get(1).executionID()).isEqualTo(e1.executionID());
	}

	@Test
	void getExecutionNodeResult_returnsResultAfterFinish() {
		UUID planID = persistPlanWithRoot();
		UUID rootNodeID = rootPlanNodeOf(planID);
		TestExecution ex = repo.newExecution(planID, Instant.now(), null);
		UUID rootExecNodeID = repo.newExecutionNode(ex.executionID(), rootNodeID);
		repo.updateExecutionNodeFinish(rootExecNodeID, ExecutionResult.PASSED, Instant.now());

		assertThat(repo.getExecutionNodeResult(rootExecNodeID)).contains(ExecutionResult.PASSED);
	}

	@Test
	void getExecutionNodeResult_returnsEmptyBeforeFinish() {
		UUID planID = persistPlanWithRoot();
		UUID rootNodeID = rootPlanNodeOf(planID);
		TestExecution ex = repo.newExecution(planID, Instant.now(), null);
		UUID rootExecNodeID = repo.newExecutionNode(ex.executionID(), rootNodeID);

		assertThat(repo.getExecutionNodeResult(rootExecNodeID)).isEmpty();
	}

	// --- full lifecycle ---

	@Test
	void fullExecutionLifecycle_singleNode() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		Instant startedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		Instant finishedAt = startedAt.plusSeconds(2);

		TestExecution execution = repo.newExecution(planID, startedAt, null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		repo.updateExecutionNodeStart(executionNodeID, startedAt);
		repo.updateExecutionNodeFinish(executionNodeID, ExecutionResult.FAILED, finishedAt);
		repo.updateExecutionNodeMessage(executionNodeID, "step failed");
		UUID attachmentID = repo.newAttachment(executionNodeID);

		assertThat(repo.getExecutionNodeByPlanNode(execution.executionID(), planNodeID)).contains(executionNodeID);
		assertThat(repo.getExecutionNodeStartedAt(executionNodeID)).contains(startedAt);
		assertThat(repo.getExecutionNodeFinishedAt(executionNodeID)).contains(finishedAt);
		assertThat(repo.getExecutionNodeResult(executionNodeID)).contains(ExecutionResult.FAILED);
		assertThat(repo.getExecutionNodeMessage(executionNodeID)).contains("step failed");
		assertThat(repo.existsAttachment(attachmentID)).isTrue();
	}

	// --- execution node stats ---

	private ExecutionNodeStats sampleStats() {
		ExecutionNodeStats stats = new ExecutionNodeStats();
		stats.numExecutions(100);
		stats.numThreads(4);
		stats.min(10);
		stats.max(500);
		stats.mean(120);
		stats.p50(100);
		stats.p95(400);
		stats.p99(480);
		stats.throughput(8.5);
		stats.errorRate(0.03);
		return stats;
	}

	@Test
	void getExecutionNodeStats_returnsEmptyWhenNoneStored() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		assertThat(repo.getExecutionNodeStats(executionNodeID)).isEmpty();
	}

	@Test
	void storeAndGetExecutionNodeStats_roundtrip() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		repo.storeExecutionNodeStats(executionNodeID, sampleStats());

		assertThat(repo.getExecutionNodeStats(executionNodeID))
			.isPresent()
			.contains(sampleStats());
	}

	@Test
	void storeExecutionNodeStats_calledTwice_updatesExisting() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		repo.storeExecutionNodeStats(executionNodeID, sampleStats());

		ExecutionNodeStats updated = sampleStats();
		updated.numExecutions(200);
		updated.max(1000);
		repo.storeExecutionNodeStats(executionNodeID, updated);

		assertThat(repo.getExecutionNodeStats(executionNodeID))
			.isPresent()
			.hasValueSatisfying(s -> {
				assertThat(s.numExecutions()).isEqualTo(200);
				assertThat(s.max()).isEqualTo(1000);
			});
	}

	@Test
	void deleteExecution_cascadesStats() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);
		repo.storeExecutionNodeStats(executionNodeID, sampleStats());

		repo.deleteExecution(execution.executionID());

		assertThat(repo.getExecutionNodeStats(executionNodeID)).isEmpty();
	}

	@Test
	void fullExecutionLifecycle_multipleNodes() {
		UUID planID = persistPlanWithRoot();
		UUID rootPlanNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		UUID casePlanNodeID = persistPlanNodeUnder(rootPlanNodeID, NodeType.TEST_CASE, "case1");
		UUID stepPlanNodeID = persistPlanNodeUnder(casePlanNodeID, NodeType.STEP, "step1");

		TestExecution execution = repo.newExecution(planID, Instant.now(), null);
		UUID rootExecNodeID = repo.newExecutionNode(execution.executionID(), rootPlanNodeID);
		UUID caseExecNodeID = repo.newExecutionNode(execution.executionID(), casePlanNodeID);
		UUID stepExecNodeID = repo.newExecutionNode(execution.executionID(), stepPlanNodeID);

		repo.updateExecutionNodeStart(rootExecNodeID, Instant.now());
		repo.updateExecutionNodeStart(caseExecNodeID, Instant.now());
		repo.updateExecutionNodeStart(stepExecNodeID, Instant.now());
		repo.updateExecutionNodeFinish(stepExecNodeID, ExecutionResult.PASSED, Instant.now());
		repo.updateExecutionNodeFinish(caseExecNodeID, ExecutionResult.PASSED, Instant.now());
		repo.updateExecutionNodeFinish(rootExecNodeID, ExecutionResult.PASSED, Instant.now());

		assertThat(repo.getExecutionNodeResult(rootExecNodeID)).contains(ExecutionResult.PASSED);
		assertThat(repo.getExecutionNodeResult(caseExecNodeID)).contains(ExecutionResult.PASSED);
		assertThat(repo.getExecutionNodeResult(stepExecNodeID)).contains(ExecutionResult.PASSED);
	}

}