package org.azertio.core.persistence;


import org.azertio.core.execution.ExecutionNodeStats;
import org.azertio.core.execution.ExecutionResult;
import org.azertio.core.execution.TestExecution;
import org.azertio.core.execution.TestExecutionNode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestExecutionRepository extends Repository {

	TestExecution newExecution(UUID planID, Instant executedAt, String profile);

	UUID newExecutionNode(UUID executionID, UUID testPlanNodeID);

	Optional<UUID> getExecutionNodeByPlanNode(UUID executionID, UUID testPlanNodeID);

	/**
	 * Retrieve all data for the execution node corresponding to the given
	 * execution and plan node.
	 */
	Optional<TestExecutionNode> getExecutionNode(UUID executionID, UUID planNodeID);

	void updateExecutionNodeStart(UUID executionNodeID, Instant startedAt);

	void updateExecutionNodeFinish(UUID executionNodeID, ExecutionResult result, Instant finishedAt);

	void updateExecutionNodeTestCounts(UUID executionNodeID, int passed, int error, int failed);

	void updateExecutionTestCounts(UUID executionID, int passed, int error, int failed);

	void updateExecutionNodeMessage(UUID executionNodeID, String message);

	UUID newAttachment(UUID executionNodeID);

	List<UUID> listAttachmentIds(UUID executionNodeID);

	/**
	 * List executions for a given plan, ordered by executedAt descending.
	 * Each returned {@link TestExecution} has {@code executionRootNodeID} populated
	 * with the execution-node ID corresponding to {@code planNodeRoot} (may be null
	 * if the execution has no such execution node yet).
	 *
	 * @param planID       the plan to query
	 * @param planNodeRoot the root plan-node ID (used to locate the root execution node)
	 * @param offset       number of records to skip (for pagination)
	 * @param max          maximum records to return; 0 or negative means no limit
	 */
	List<TestExecution> listExecutions(UUID planID, UUID planNodeRoot, int offset, int max);

	Optional<TestExecution> getExecution(UUID executionId);

	/**
	 * Retrieve the result of a single execution node.
	 *
	 * @param executionNodeID the execution node ID
	 * @return the result, or empty if the node has not finished yet
	 */
	Optional<ExecutionResult> getExecutionNodeResult(UUID executionNodeID);

	/**
	 * Delete a single execution and all its nodes and attachment records.
	 * File-system attachments must be removed separately via {@link org.azertio.core.persistence.AttachmentRepository}.
	 *
	 * @param executionId the execution to delete
	 */
	void deleteExecution(UUID executionId);

	/**
	 * Delete all executions belonging to the given plan, including their nodes and attachment records.
	 * File-system attachments must be removed separately via {@link org.azertio.core.persistence.AttachmentRepository}.
	 *
	 * @param planId the plan whose executions should be deleted
	 */
	void deleteExecutionsByPlan(UUID planId);

	default Optional<ExecutionNodeStats> getExecutionNodeStats(UUID executionNodeID) {
		return Optional.empty();
	}

	default void storeExecutionNodeStats(UUID executionNodeID, ExecutionNodeStats stats) {
		// no-op for repositories that don't support stats
	}
}
