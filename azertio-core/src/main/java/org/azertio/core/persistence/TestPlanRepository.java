package org.azertio.core.persistence;

import org.azertio.core.AzertioException;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestPlanNode;
import org.azertio.core.testplan.TestProject;
import org.azertio.core.testplan.ValidationStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;


/**
 * Repository interface for managing plan nodes. Necessary for working with large test plan models
 * that cannot be fully loaded into memory.
 * This interface defines methods for retrieving, persisting, and manipulating plan nodes
 * in a storage system. It supports operations such as attaching and detaching child nodes,
 * searching nodes based on criteria, and managing node relationships.
 * Implementations of this interface should handle the underlying storage and retrieval logic,
 * allowing for flexibility in how plan nodes are stored (e.g., in-memory, database, etc.).
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
public interface TestPlanRepository extends Repository {


	/**
	 * Retrieve the data of a plan node by its ID.
	 * @param id the node ID
	 * @return the plan node data, or empty if the node does not exist
	 */
	Optional<TestPlanNode> getNodeData(UUID id);

	/**
	 * Update a specific field of a plan node. The field name and value are determined by the caller,
	 * and the repository implementation should handle the storage and retrieval of these fields.
	 * @param id the node ID
	 * @param fieldName the name of the field to update
	 * @param fieldValue the new value for the field
	 * @throws AzertioException if the node does not exist in the repository
	 */
	<T> void updateNodeField(UUID id, String fieldName, T fieldValue);


	/**
	 * Retrieve the value of a specific field of a plan node.
	 * @param id the node ID
	 * @param fieldName the name of the field to retrieve
	 * @return the value of the field, or empty if the node or field does not exist
	 * @throws AzertioException if the node does not exist in the repository
	 */
	<T> Optional<T> getNodeField(UUID id, String fieldName);


	/**
	 * Check whether a plan node exists in the repository.
	 * @param id the node ID
	 * @return {@code true} if the node exists, {@code false} otherwise
	 */
	boolean existsNode(UUID id);

	/**
	 * Retrieve the parent node of a given node.
	 * @param id the node ID
	 * @return the parent node ID, or empty if the node is a root node
	 * @throws AzertioException if the node does not exist in the repository
	 */
	Optional<UUID> getParentNode(UUID id);

	/**
	 * Delete completely a plan node, including its child nodes.
	 * If the node was a child of another node, it will be detached.
	 * @throws AzertioException if the node does not exist in the repository
	 */
	void deleteNode(UUID id);


	/**
	 * Attach a plan node as child of another node, at the end of the existing child list.
	 * If the child node was already in the child list, this operation will have no effect.
	 * @throws AzertioException if either the parent UUID or the child UUID do not exist in the repository
	 */
	void attachChildNodeLast(UUID parent, UUID child);

	/**
	 * Attach a plan node as child of another node, at the beginning of the existing child list.
	 * If the child node was already in the child list, this operation will have no effect.
	 * @throws AzertioException if either the parent UUID or the child UUID do not exist in the repository
	 */
	void attachChildNodeFirst(UUID parent, UUID child);


	/**
	 * Detach a plan node as a child of another node, keeping it in the repository as an orphan node.
	 * If the child node was not already in the child list, this operation will have no effect.
	 * @throws AzertioException if either the parent UUID or the child UUID do not exist in the repository
	 */
	void detachChildNode(UUID parent, UUID child);



	/**
	 * Retrieve the direct children of a node, ordered by their position.
	 * @param id the parent node ID
	 * @return a stream of child node IDs in order
	 * @throws AzertioException if the node does not exist in the repository
	 */
	Stream<UUID> getNodeChildren(UUID id);

	/**
	 * Retrieve all descendants of a node (children, grandchildren, etc.) recursively.
	 * @param id the ancestor node ID
	 * @return a stream of descendant node IDs
	 * @throws AzertioException if the node does not exist in the repository
	 */
	Stream<UUID> getNodeDescendants(UUID id);

	/**
	 * Retrieve all nodes in the subtree rooted at {@code rootNodeId} (including the root itself)
	 * that have their own validation error (i.e. {@code VALIDATION_STATUS} is not OK).
	 * Nodes that carry {@code HAS_ISSUES=true} only because of a child are not included.
	 * @param rootNodeId the root of the subtree to inspect
	 * @return a stream of node IDs that have a validation error
	 */
	Stream<UUID> getNodeDescendantsWithIssues(UUID rootNodeId);

	/**
	 * Retrieve all ancestors of a node (parent, grandparent, etc.) up to the root.
	 * @param id the descendant node ID
	 * @return a stream of ancestor node IDs
	 * @throws AzertioException if the node does not exist in the repository
	 */
	Stream<UUID> getNodeAncestors(UUID id);

	/**
	 * Count the direct children of a node.
	 * @param id the parent node ID
	 * @return the number of direct children
	 * @throws AzertioException if the node does not exist in the repository
	 */
	int countNodeChildren(UUID id);

	/**
	 * Count all descendants of a node recursively.
	 * @param id the ancestor node ID
	 * @return the total number of descendants
	 * @throws AzertioException if the node does not exist in the repository
	 */
	int countNodeDescendants(UUID id);

	/**
	 * Count all ancestors of a node up to the root.
	 * @param id the descendant node ID
	 * @return the total number of ancestors
	 * @throws AzertioException if the node does not exist in the repository
	 */
	int countNodeAncestors(UUID id);

	/**
	 * Persist a plan node in the repository. If the node UUID did exist previously, it
	 * will update the node content; otherwise, it will create a new record and assign a
	 * unique UUID.
	 * @param node the plan node to persist
	 * @return the assigned node UUID
	 */
	UUID persistNode(TestPlanNode node);

	/**
	 * Search for nodes matching the given criteria.
	 * @param criteria the search criteria
	 * @return a stream of matching node IDs
	 */
	Stream<UUID> searchNodes(TestPlanNodeCriteria criteria);

	/**
	 * Count nodes matching the given criteria.
	 * @param criteria the search criteria
	 * @return the number of matching nodes
	 */
	int countNodes(TestPlanNodeCriteria criteria);

	/**
	 * Check whether a node has a specific tag.
	 * @param nodeID the node ID
	 * @param tag the tag to check
	 * @return {@code true} if the tag exists on the node, {@code false} otherwise
	 */
	boolean existsNodeTag(UUID nodeID, String tag);

	/**
	 * Retrieve all tags of a node.
	 * @param nodeID the node ID
	 * @return a stream of tags associated with the node, or an empty stream if the node has no tags
	 */
	void addNodeTag(UUID nodeID, String tag);

	/**
	 * Remove a specific tag from a node. If the tag does not exist on the node, this operation will have no effect.
	 * @param nodeID the node ID
	 * @param tag the tag to remove
	 */
	void removeNodeTag(UUID nodeID, String tag);

	/**
	 * Retrieve all tags of a node as a list.
	 * @param nodeID the node ID
	 * @return a list of tags associated with the node, or an empty list if the node has no tags
	 */
	List<String> getNodeTags(UUID nodeID);

	/**
	 * Check whether a node has a specific property. If {@code propertyValue} is {@code null},
	 * only the existence of the key is checked regardless of its value.
	 * @param nodeID the node ID
	 * @param propertyKey the property key
	 * @param propertyValue the expected value, or {@code null} to match any value
	 * @return {@code true} if the property exists (and matches the value if provided)
	 */
	boolean existsNodeProperty(UUID nodeID, String propertyKey, String propertyValue);

	void addNodeProperty(UUID nodeID, String propertyKey, String propertyValue);
	void removeNodeProperty(UUID nodeID, String propertyKey);

	/**
	 * Retrieve the value of a specific property of a node.
	 * @param nodeID the node ID
	 * @param propertyKey the property key
	 * @return the property value, or empty if the property does not exist
	 */
	Optional<String> getNodeProperty(UUID nodeID, String propertyKey);

	/**
	 * Retrieve all properties of a node as a map of key-value pairs.
	 * @param nodeID the node ID
	 * @return a map containing all properties of the node, or an empty map if the node has no properties
	 */
	Map<String, String> getNodeProperties(UUID nodeID);


	List<TestPlan> listPlans();

	/**
	 * List test plans for a given organization and project, ordered by creation date descending.
	 * @param organization the organization name
	 * @param project the project name
	 * @param offset number of records to skip (for pagination)
	 * @param max maximum number of records to return (0 or negative means no limit)
	 * @return a list of matching test plans ordered by createdAt descending
	 */
	List<TestPlan> listPlans(String organization, String project, int offset, int max);

	/**
	 * List test plans for a given organization and project, optionally filtering to only those
	 * that have at least one execution.
	 */
	default List<TestPlan> listPlans(String organization, String project, int offset, int max, boolean withExecutions) {
		return listPlans(organization, project, offset, max);
	}

	/**
	 * For every non-TEST_CASE node belonging to the given plan, compute the number of
	 * descendant TEST_CASE nodes and persist it as {@code testCaseCount}.
	 * TEST_CASE nodes (and their descendants) are left with {@code null}.
	 */
	default void assignTestCaseCountsToNodes(UUID planId) {
		// no-op default; concrete repositories may override
	}

	Optional<TestPlan> getPlan(TestProject testProject, String resourceSetHash, String configurationHash);

	Optional<TestPlan> getPlan(UUID planID);

	Optional<TestProject> getProject(UUID projectID);

	TestPlan persistPlan(TestPlan testPlan);

	UUID persistProject(TestProject testProject);

	/**
	 * Assign the given plan ID to the root node and all its descendants.
	 * @param planId the plan ID to assign
	 * @param rootNodeId the root node of the plan
	 */
	void assignPlanToNodes(UUID planId, UUID rootNodeId);

	/**
	 * Persist the validation result of a single node.
	 * @param nodeId the node to update
	 * @param status the validation status
	 * @param message human-readable error message, or {@code null} if OK
	 */
	void setNodeValidation(UUID nodeId, ValidationStatus status, String message);

	/**
	 * After all nodes in the plan have been individually validated, propagate
	 * {@code HAS_ISSUES=true} upward so that every ancestor of a failing node
	 * also reflects that it has issues in its subtree.
	 * @param planId the plan whose nodes should be updated
	 */
	void propagatePlanIssues(UUID planId);

	/**
	 * Returns {@code true} if any node in the plan has {@code HAS_ISSUES=true}.
	 * @param planId the plan to check
	 */
	boolean planHasIssues(UUID planId);

	/**
	 * Delete a plan and all its nodes (including tags and properties).
	 * Executions must be deleted separately via {@link TestExecutionRepository}.
	 *
	 * @param planId the plan to delete
	 */
	void deletePlan(UUID planId);

}
