package org.azertio.test;

import org.azertio.core.AzertioRuntime;
import org.azertio.core.execution.ExecutionResult;
import org.azertio.core.execution.TestExecution;
import org.azertio.core.persistence.TestExecutionRepository;
import org.azertio.core.persistence.TestPlanNodeCriteria;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.NodeType;
import org.azertio.core.testplan.TestPlan;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Result of a full Azertio test plan execution, returned by {@link JUnitAzertioPlan#execute()}.
 * Provides fluent assertion methods over all test cases in the executed plan.
 */
public class JUnitAzertioResult {

    private final AzertioRuntime runtime;
    private final TestPlan plan;
    private final TestExecution execution;

    JUnitAzertioResult(AzertioRuntime runtime, TestPlan plan, TestExecution execution) {
        this.runtime = runtime;
        this.plan = plan;
        this.execution = execution;
    }

    public JUnitAzertioResult assertAllPassed() {
        return assertAll(ExecutionResult.PASSED);
    }

    public JUnitAzertioResult assertAllFailed() {
        return assertAll(ExecutionResult.FAILED);
    }

    public TestExecution execution() {
        return execution;
    }

    public AzertioRuntime runtime() {
        return runtime;
    }

    private JUnitAzertioResult assertAll(ExecutionResult expected) {
        TestPlanRepository planRepo = runtime.getRepository(TestPlanRepository.class);
        TestExecutionRepository execRepo = runtime.getRepository(TestExecutionRepository.class);

        planRepo.searchNodes(TestPlanNodeCriteria.and(
            TestPlanNodeCriteria.descendantOf(plan.planNodeRoot()),
            TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE)
        )).forEach(nodeId -> {
            UUID execNodeId = execRepo
                .getExecutionNodeByPlanNode(execution.executionID(), nodeId)
                .orElseThrow(() -> new AssertionError("No execution node found for plan node " + nodeId));
            assertThat(execRepo.getExecutionNodeResult(execNodeId))
                .as("result for test case node %s", nodeId)
                .contains(expected);
        });

        return this;
    }
}
