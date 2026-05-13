package org.azertio.persistence.test.plan;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.azertio.core.persistence.TestPlanNodeCriteria;
import org.azertio.core.testplan.*;
import org.azertio.persistence.DataSourceProvider;
import org.azertio.persistence.plan.JooqPlanRepository;
import javax.sql.DataSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractRepositoryTest {

	protected JooqPlanRepository repo;
	private DataSource dataSource;

	protected abstract DataSourceProvider dataSourceProvider();

	@BeforeEach
	void setUp() {
		DataSourceProvider provider = dataSourceProvider();
		dataSource = provider.obtainDataSource();
		repo = new JooqPlanRepository(dataSource, provider.dialect());
		repo.clearAllData();
	}

	@AfterEach
	void tearDown() {
		if (dataSource instanceof HikariDataSource hikari) {
			hikari.close();
		}
	}

	@Test
	void insertPlanNodeWithAllFields() {
		DataTable dataTable = new DataTable(List.of(
			List.of("col1", "col2"),
			List.of("val1", "val2")
		));
		Document document = Document.of("text/plain", "document content");

		TestPlanNode node = new TestPlanNode()
			.nodeType(NodeType.TEST_PLAN)
			.name("test name")
			.language("en")
			.identifier("TC-001")
			.source("test.feature")
			.keyword("Scenario")
			.description("test description")
			.display("Test Display")
			.dataTable(dataTable)
			.document(document)
			.tags(new HashSet<>(Set.of("smoke", "regression")))
			.properties(new TreeMap<>(Map.of("priority", "high", "author", "tester")));

		UUID id = repo.persistNode(node);

		assertThat(id).isNotNull();
		assertThat(repo.existsNode(id)).isTrue();

		TestPlanNode retrieved = repo.getNodeData(id).orElseThrow();

		assertThat(retrieved.nodeType()).isEqualTo(NodeType.TEST_PLAN);
		assertThat(retrieved.name()).isEqualTo("test name");
		assertThat(retrieved.language()).isEqualTo("en");
		assertThat(retrieved.identifier()).isEqualTo("TC-001");
		assertThat(retrieved.source()).isEqualTo("test.feature");
		assertThat(retrieved.keyword()).isEqualTo("Scenario");
		assertThat(retrieved.description()).isEqualTo("test description");
		assertThat(retrieved.display()).isEqualTo("Test Display");
		assertThat(retrieved.dataTable()).isEqualTo(dataTable);
		assertThat(retrieved.document().content()).isEqualTo("document content");
		assertThat(retrieved.document().mimeType()).isEqualTo("text/plain");
		assertThat(retrieved.tags()).containsExactlyInAnyOrder("smoke", "regression");
		assertThat(retrieved.properties()).containsEntry("priority", "high");
		assertThat(retrieved.properties()).containsEntry("author", "tester");
	}

	@Test
	void insertPlanNodeWithMinimalFields() {
		TestPlanNode node = new TestPlanNode()
			.nodeType(NodeType.STEP)
			.name("minimal node");

		UUID id = repo.persistNode(node);

		assertThat(id).isNotNull();
		TestPlanNode retrieved = repo.getNodeData(id).orElseThrow();
		assertThat(retrieved.name()).isEqualTo("minimal node");
		assertThat(retrieved.nodeType()).isEqualTo(NodeType.STEP);
		assertThat(retrieved.description()).isNull();
		assertThat(retrieved.dataTable()).isNull();
		assertThat(retrieved.document()).isNull();
	}

	@Test
	void updateAndRetrieveFields() {
		TestPlanNode node = new TestPlanNode()
			.nodeType(NodeType.TEST_CASE)
			.name("original name")
			.description("original description");

		UUID id = repo.persistNode(node);

		String retrievedName = repo.getNodeField(id, "name").orElseThrow().toString();
		assertThat(retrievedName).isEqualTo("original name");
		repo.updateNodeField(id, "name", "updated name");
		String updatedName = repo.getNodeField(id, "name").orElseThrow().toString();
		assertThat(updatedName).isEqualTo("updated name");
	}

	@Test
	void updatePlanNode() {
		TestPlanNode node = new TestPlanNode()
			.nodeType(NodeType.TEST_CASE)
			.name("original name")
			.description("original description");

		UUID id = repo.persistNode(node);

		TestPlanNode toUpdate = repo.getNodeData(id).orElseThrow();
		toUpdate.name("updated name");
		toUpdate.description("updated description");
		toUpdate.tags().add("new-tag");

		repo.persistNode(toUpdate);

		TestPlanNode updated = repo.getNodeData(id).orElseThrow();
		assertThat(updated.name()).isEqualTo("updated name");
		assertThat(updated.description()).isEqualTo("updated description");
		assertThat(updated.tags()).contains("new-tag");
	}

	@Test
	void deletePlanNode() {
		TestPlanNode node = new TestPlanNode()
			.nodeType(NodeType.STEP)
			.name("to delete");

		UUID id = repo.persistNode(node);
		assertThat(repo.existsNode(id)).isTrue();

		repo.deleteNode(id);

		assertThat(repo.existsNode(id)).isFalse();
		assertThat(repo.getNodeData(id)).isEmpty();
	}

	@Test
	void attachChildNodeLast_buildsTreeHierarchy() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID child1 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		UUID child2 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child2"));
		UUID child3 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child3"));

		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeLast(root, child2);
		repo.attachChildNodeLast(root, child3);

		List<UUID> children = repo.getNodeChildren(root).toList();
		assertThat(children).containsExactly(child1, child2, child3);

		assertThat(repo.getParentNode(child1)).contains(root);
		assertThat(repo.getParentNode(child2)).contains(root);
		assertThat(repo.getParentNode(child3)).contains(root);

	}

	@Test
	void attachChildNodeFirst_insertsAtBeginning() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID child1 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		UUID child2 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child2"));
		UUID child3 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child3"));

		repo.attachChildNodeFirst(root, child1);
		repo.attachChildNodeFirst(root, child2);
		repo.attachChildNodeFirst(root, child3);

		List<UUID> children = repo.getNodeChildren(root).toList();
		assertThat(children).containsExactly(child3, child2, child1);
	}

	@Test
	void detachChildNode_removesFromParent() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID child1 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		UUID child2 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child2"));

		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeLast(root, child2);

		repo.detachChildNode(root, child1);

		List<UUID> children = repo.getNodeChildren(root).toList();
		assertThat(children).containsExactly(child2);
		assertThat(repo.getParentNode(child1)).isEmpty();
	}

	@Test
	void deepTreeHierarchy_threeLevels() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID level1a = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("level1a"));
		UUID level1b = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("level1b"));
		UUID level2a = repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("level2a"));
		UUID level2b = repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("level2b"));

		repo.attachChildNodeLast(root, level1a);
		repo.attachChildNodeLast(root, level1b);
		repo.attachChildNodeLast(level1a, level2a);
		repo.attachChildNodeLast(level1a, level2b);

		assertThat(repo.getNodeChildren(root).toList()).containsExactly(level1a, level1b);
		assertThat(repo.getNodeChildren(level1a).toList()).containsExactly(level2a, level2b);
		assertThat(repo.getNodeChildren(level1b).toList()).isEmpty();

		assertThat(repo.getParentNode(level2a)).contains(level1a);
	}

	@Test
	void moveNodeBetweenParents() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID parent1 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("parent1"));
		UUID parent2 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("parent2"));
		UUID child = repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("child"));

		repo.attachChildNodeLast(root, parent1);
		repo.attachChildNodeLast(root, parent2);
		repo.attachChildNodeLast(parent1, child);

		assertThat(repo.getNodeChildren(parent1).toList()).containsExactly(child);
		assertThat(repo.getNodeChildren(parent2).toList()).isEmpty();

		repo.detachChildNode(parent1, child);
		repo.attachChildNodeLast(parent2, child);

		assertThat(repo.getNodeChildren(parent1).toList()).isEmpty();
		assertThat(repo.getNodeChildren(parent2).toList()).containsExactly(child);
		assertThat(repo.getParentNode(child)).contains(parent2);
	}

	@Test
	void mixedAttachOperations_maintainsCorrectOrder() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID child1 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		UUID child2 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child2"));
		UUID child3 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child3"));
		UUID child4 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child4"));

		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeFirst(root, child2);
		repo.attachChildNodeLast(root, child3);
		repo.attachChildNodeFirst(root, child4);

		List<UUID> children = repo.getNodeChildren(root).toList();
		assertThat(children).containsExactly(child4, child2, child1, child3);
	}

	@Test
	void deleteNodeWithChildren_cascadesDeletion() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID child1 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		UUID child2 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child2"));

		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeLast(root, child2);

		repo.deleteNode(root);

		assertThat(repo.existsNode(root)).isFalse();
		assertThat(repo.existsNode(child1)).isFalse();
		assertThat(repo.existsNode(child2)).isFalse();
	}

	@Test
	void deleteNodeWithMultipleParents_doesNotDeleteSharedNode() {
		UUID root1 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root1"));
		UUID root2 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root2"));
		UUID sharedChild = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("sharedChild"));

		repo.attachChildNodeLast(root1, sharedChild);
		repo.attachChildNodeLast(root2, sharedChild);

		repo.deleteNode(root1);

		assertThat(repo.existsNode(root1)).isFalse();
		assertThat(repo.existsNode(sharedChild)).isTrue();
		assertThat(repo.getParentNode(sharedChild)).contains(root2);
	}

	@Test
	void countNodeChildren_returnsCorrectCount() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		assertThat(repo.countNodeChildren(root)).isZero();

		UUID child1 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		UUID child2 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child2"));
		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeLast(root, child2);

		assertThat(repo.countNodeChildren(root)).isEqualTo(2);
	}

	@Test
	void getNodeDescendants_returnsAllDescendants() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID child = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child"));
		UUID grandchild = repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("grandchild"));

		repo.attachChildNodeLast(root, child);
		repo.attachChildNodeLast(child, grandchild);

		List<UUID> descendants = repo.getNodeDescendants(root).toList();
		assertThat(descendants).containsExactlyInAnyOrder(child, grandchild);
	}

	@Test
	void getNodeDescendants_emptyForLeafNode() {
		UUID leaf = repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("leaf"));
		assertThat(repo.getNodeDescendants(leaf).toList()).isEmpty();
	}

	@Test
	void countNodeDescendants_returnsCorrectCount() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID child = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child"));
		UUID grandchild = repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("grandchild"));

		repo.attachChildNodeLast(root, child);
		repo.attachChildNodeLast(child, grandchild);

		assertThat(repo.countNodeDescendants(root)).isEqualTo(2);
		assertThat(repo.countNodeDescendants(child)).isEqualTo(1);
		assertThat(repo.countNodeDescendants(grandchild)).isZero();
	}

	@Test
	void getNodeAncestors_returnsAllAncestors() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID child = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child"));
		UUID grandchild = repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("grandchild"));

		repo.attachChildNodeLast(root, child);
		repo.attachChildNodeLast(child, grandchild);

		List<UUID> ancestors = repo.getNodeAncestors(grandchild).toList();
		assertThat(ancestors).containsExactlyInAnyOrder(child, root);
	}

	@Test
	void getNodeAncestors_emptyForRootNode() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		assertThat(repo.getNodeAncestors(root).toList()).isEmpty();
	}

	@Test
	void existsTag_returnsTrueForExistingTag() {
		TestPlanNode node = new TestPlanNode()
			.nodeType(NodeType.TEST_CASE)
			.name("tagged")
			.tags(new HashSet<>(Set.of("smoke", "regression")));
		UUID id = repo.persistNode(node);

		assertThat(repo.existsNodeTag(id, "smoke")).isTrue();
		assertThat(repo.existsNodeTag(id, "regression")).isTrue();
		assertThat(repo.existsNodeTag(id, "nonexistent")).isFalse();
	}

	@Test
	void existsProperty_returnsTrueForExistingProperty() {
		TestPlanNode node = new TestPlanNode()
			.nodeType(NodeType.TEST_CASE)
			.name("with props")
			.properties(new TreeMap<>(Map.of("priority", "high", "author", "tester")));
		UUID id = repo.persistNode(node);

		assertThat(repo.existsNodeProperty(id, "priority", "high")).isTrue();
		assertThat(repo.existsNodeProperty(id, "priority", "low")).isFalse();
		assertThat(repo.existsNodeProperty(id, "priority", null)).isTrue();
		assertThat(repo.existsNodeProperty(id, "missing", null)).isFalse();
	}

	@Test
	void getNodeProperty_returnsValue() {
		TestPlanNode node = new TestPlanNode()
			.nodeType(NodeType.TEST_CASE)
			.name("with props")
			.properties(new TreeMap<>(Map.of("priority", "high")));
		UUID id = repo.persistNode(node);

		assertThat(repo.getNodeProperty(id, "priority")).contains("high");
		assertThat(repo.getNodeProperty(id, "missing")).isEmpty();
	}

	@Test
	void searchNodes_byNodeType() {
		repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("plan"));
		repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("case1"));
		repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("case2"));
		repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("step"));

		List<UUID> results = repo.searchNodes(
			TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE)
		).toList();
		assertThat(results).hasSize(2);
	}

	@Test
	void searchNodes_byTag() {
		TestPlanNode tagged = new TestPlanNode().nodeType(NodeType.TEST_CASE).name("tagged")
			.tags(new HashSet<>(Set.of("smoke")));
		TestPlanNode untagged = new TestPlanNode().nodeType(NodeType.TEST_CASE).name("untagged");
		repo.persistNode(tagged);
		repo.persistNode(untagged);

		List<UUID> results = repo.searchNodes(
			TestPlanNodeCriteria.withTag("smoke")
		).toList();
		assertThat(results).hasSize(1);
	}

	@Test
	void searchNodes_byProperty() {
		TestPlanNode withProp = new TestPlanNode().nodeType(NodeType.TEST_CASE).name("with")
			.properties(new TreeMap<>(Map.of("env", "prod")));
		TestPlanNode without = new TestPlanNode().nodeType(NodeType.TEST_CASE).name("without");
		repo.persistNode(withProp);
		repo.persistNode(without);

		assertThat(repo.searchNodes(TestPlanNodeCriteria.withProperty("env", "prod")).toList()).hasSize(1);
		assertThat(repo.searchNodes(TestPlanNodeCriteria.withProperty("env", null)).toList()).hasSize(1);
		assertThat(repo.searchNodes(TestPlanNodeCriteria.withProperty("env", "dev")).toList()).isEmpty();
	}

	@Test
	void searchNodes_byField() {
		repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("with lang").language("en"));
		repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("no lang"));

		assertThat(repo.searchNodes(TestPlanNodeCriteria.withField("language", "en")).toList()).hasSize(1);
		assertThat(repo.searchNodes(TestPlanNodeCriteria.withField("language")).toList()).hasSize(1);
	}

	@Test
	void searchNodes_withAndOrNot() {
		repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("case")
			.tags(new HashSet<>(Set.of("smoke"))));
		repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("step")
			.tags(new HashSet<>(Set.of("smoke"))));
		repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("case2"));

		List<UUID> andResult = repo.searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			TestPlanNodeCriteria.withTag("smoke")
		)).toList();
		assertThat(andResult).hasSize(1);

		List<UUID> orResult = repo.searchNodes(TestPlanNodeCriteria.or(
			TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			TestPlanNodeCriteria.withNodeType(NodeType.STEP)
		)).toList();
		assertThat(orResult).hasSize(3);

		List<UUID> notResult = repo.searchNodes(TestPlanNodeCriteria.not(
			TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE)
		)).toList();
		assertThat(notResult).hasSize(1);
	}

	@Test
	void searchNodes_all() {
		repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("a"));
		repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("b"));

		assertThat(repo.searchNodes(TestPlanNodeCriteria.all()).toList()).hasSize(2);
	}

	@Test
	void countNodes_returnsCorrectCount() {
		repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("case1"));
		repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("case2"));
		repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("step"));

		assertThat(repo.countNodes(TestPlanNodeCriteria.all())).isEqualTo(3);
		assertThat(repo.countNodes(TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE))).isEqualTo(2);
		assertThat(repo.countNodes(TestPlanNodeCriteria.withNodeType(NodeType.STEP))).isEqualTo(1);
	}



	@Test
	void moveSubtreeBetweenRoots_updatesAllDescendantRootNodes() {
		UUID root1 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root1"));
		UUID root2 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root2"));
		UUID child = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child"));
		UUID grandchild = repo.persistNode(new TestPlanNode().nodeType(NodeType.STEP).name("grandchild"));

		repo.attachChildNodeLast(root1, child);
		repo.attachChildNodeLast(child, grandchild);

		// Move subtree from root1 to root2
		repo.detachChildNode(root1, child);
		repo.attachChildNodeLast(root2, child);

		// Descendants query should work correctly from root2
		assertThat(repo.getNodeDescendants(root2).toList()).containsExactlyInAnyOrder(child, grandchild);
		assertThat(repo.getNodeDescendants(root1).toList()).isEmpty();
	}

	@Test
	void persistProject_createsNewProject() {
		TestProject testProject = new TestProject("MyProject", "desc", "MyOrg", List.of());

		UUID id = repo.persistProject(testProject);

		assertThat(id).isNotNull();
	}

	@Test
	void persistProject_returnsExistingIdForDuplicateProject() {
		TestProject testProject = new TestProject("MyProject", "desc", "MyOrg", List.of());

		UUID first = repo.persistProject(testProject);
		UUID second = repo.persistProject(testProject);

		assertThat(first).isEqualTo(second);
	}

	@Test
	void persistProject_differentOrganizationsAreDistinct() {
		TestProject p1 = new TestProject("MyProject", "desc", "OrgA", List.of());
		TestProject p2 = new TestProject("MyProject", "desc", "OrgB", List.of());

		UUID id1 = repo.persistProject(p1);
		UUID id2 = repo.persistProject(p2);

		assertThat(id1).isNotEqualTo(id2);
	}

	@Test
	void persistPlan_insertsAndAssignsId() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		TestProject testProject = new TestProject("MyProject", "desc", "MyOrg", List.of());
		UUID projectID = repo.persistProject(testProject);

		TestPlan testPlan = new TestPlan(null, projectID, Instant.now(), "rHash", "cHash", root, 0, null);
		TestPlan saved = repo.persistPlan(testPlan);

		assertThat(saved.planID()).isNotNull();
		assertThat(saved.projectID()).isEqualTo(projectID);
		assertThat(saved.resourceSetHash()).isEqualTo("rHash");
		assertThat(saved.configurationHash()).isEqualTo("cHash");
		assertThat(saved.planNodeRoot()).isEqualTo(root);
	}

	@Test
	void persistPlan_withExplicitId_usesProvidedId() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		TestProject testProject = new TestProject("MyProject", "desc", "MyOrg", List.of());
		UUID projectID = repo.persistProject(testProject);
		UUID explicitID = UUID.randomUUID();

		TestPlan testPlan = new TestPlan(explicitID, projectID, Instant.now(), "rHash", "cHash", root, 0, null);
		TestPlan saved = repo.persistPlan(testPlan);

		assertThat(saved.planID()).isEqualTo(explicitID);
	}

	@Test
	void getPlan_byProjectAndHashes_returnsMatchingPlan() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		TestProject testProject = new TestProject("MyProject", "desc", "MyOrg", List.of());
		UUID projectID = repo.persistProject(testProject);
		Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

		TestPlan testPlan = new TestPlan(null, projectID, now, "rHash", "cHash", root, 0, null);
		TestPlan saved = repo.persistPlan(testPlan);

		Optional<TestPlan> found = repo.getPlan(testProject, "rHash", "cHash");

		assertThat(found).isPresent();
		assertThat(found.get().planID()).isEqualTo(saved.planID());
		assertThat(found.get().planNodeRoot()).isEqualTo(root);
		assertThat(found.get().createdAt()).isEqualTo(now);
	}

	@Test
	void getPlan_byProjectAndHashes_returnsEmptyWhenHashesDontMatch() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		TestProject testProject = new TestProject("MyProject", "desc", "MyOrg", List.of());
		UUID projectID = repo.persistProject(testProject);

		repo.persistPlan(new TestPlan(null, projectID, Instant.now(), "rHash", "cHash", root, 0, null));

		assertThat(repo.getPlan(testProject, "otherHash", "cHash")).isEmpty();
		assertThat(repo.getPlan(testProject, "rHash", "otherHash")).isEmpty();
	}

	@Test
	void getPlan_byProjectAndHashes_returnsEmptyWhenProjectNotFound() {
		TestProject unknown = new TestProject("Unknown", "desc", "NoOrg", List.of());

		assertThat(repo.getPlan(unknown, "rHash", "cHash")).isEmpty();
	}

	@Test
	void getPlan_byUUID_returnsMatchingPlan() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		TestProject testProject = new TestProject("MyProject", "desc", "MyOrg", List.of());
		UUID projectID = repo.persistProject(testProject);

		TestPlan saved = repo.persistPlan(new TestPlan(null, projectID, Instant.now(), "rHash", "cHash", root, 0, null));

		Optional<TestPlan> found = repo.getPlan(saved.planID());

		assertThat(found).isPresent();
		assertThat(found.get().planID()).isEqualTo(saved.planID());
		assertThat(found.get().planNodeRoot()).isEqualTo(root);
	}

	@Test
	void getPlan_byUUID_returnsEmptyForUnknownId() {
		assertThat(repo.getPlan(UUID.randomUUID())).isEmpty();
	}

	// --- listPlans(organization, project, offset, max) ---

	private TestPlan persistPlanForProject(String organization, String project, Instant createdAt) {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		TestProject testProject = new TestProject(project, "desc", organization, List.of());
		UUID projectId = repo.persistProject(testProject);
		return repo.persistPlan(new TestPlan(null, projectId, createdAt, "rHash", "cHash", root, 0, null));
	}

	@Test
	void listPlans_returnsPlansForMatchingOrgAndProject() {
		persistPlanForProject("OrgA", "ProjA", Instant.now());
		persistPlanForProject("OrgA", "ProjB", Instant.now());
		persistPlanForProject("OrgB", "ProjA", Instant.now());

		assertThat(repo.listPlans("OrgA", "ProjA", 0, 0)).hasSize(1);
	}

	@Test
	void listPlans_returnsEmptyWhenNoMatch() {
		assertThat(repo.listPlans("NonExistent", "NonExistent", 0, 0)).isEmpty();
	}

	@Test
	void listPlans_doesNotReturnPlansFromOtherProjects() {
		persistPlanForProject("OrgA", "ProjB", Instant.now());
		persistPlanForProject("OrgB", "ProjA", Instant.now());

		assertThat(repo.listPlans("OrgA", "ProjA", 0, 0)).isEmpty();
	}

	@Test
	void listPlans_orderedByCreatedAtDescending() {
		Instant t1 = Instant.now().minusSeconds(200).truncatedTo(ChronoUnit.MILLIS);
		Instant t2 = Instant.now().minusSeconds(100).truncatedTo(ChronoUnit.MILLIS);
		Instant t3 = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		persistPlanForProject("OrgA", "ProjA", t1);
		persistPlanForProject("OrgA", "ProjA", t2);
		persistPlanForProject("OrgA", "ProjA", t3);

		List<TestPlan> result = repo.listPlans("OrgA", "ProjA", 0, 0);
		assertThat(result).hasSize(3);
		assertThat(result.get(0).createdAt()).isEqualTo(t3);
		assertThat(result.get(1).createdAt()).isEqualTo(t2);
		assertThat(result.get(2).createdAt()).isEqualTo(t1);
	}

	@Test
	void listPlans_withMax_limitsResults() {
		persistPlanForProject("OrgA", "ProjA", Instant.now().minusSeconds(200));
		persistPlanForProject("OrgA", "ProjA", Instant.now().minusSeconds(100));
		persistPlanForProject("OrgA", "ProjA", Instant.now());

		assertThat(repo.listPlans("OrgA", "ProjA", 0, 2)).hasSize(2);
	}

	@Test
	void listPlans_withMaxZero_returnsAll() {
		persistPlanForProject("OrgA", "ProjA", Instant.now().minusSeconds(200));
		persistPlanForProject("OrgA", "ProjA", Instant.now().minusSeconds(100));
		persistPlanForProject("OrgA", "ProjA", Instant.now());

		assertThat(repo.listPlans("OrgA", "ProjA", 0, 0)).hasSize(3);
	}

	@Test
	void listPlans_withOffset_skipsRecords() {
		Instant t1 = Instant.now().minusSeconds(200).truncatedTo(ChronoUnit.MILLIS);
		Instant t2 = Instant.now().minusSeconds(100).truncatedTo(ChronoUnit.MILLIS);
		Instant t3 = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		persistPlanForProject("OrgA", "ProjA", t1);
		persistPlanForProject("OrgA", "ProjA", t2);
		persistPlanForProject("OrgA", "ProjA", t3);

		// With desc order: [t3, t2, t1]. Skipping 1 -> [t2, t1]
		List<TestPlan> result = repo.listPlans("OrgA", "ProjA", 1, 0);
		assertThat(result).hasSize(2);
		assertThat(result.get(0).createdAt()).isEqualTo(t2);
		assertThat(result.get(1).createdAt()).isEqualTo(t1);
	}

	@Test
	void listPlans_withOffsetAndMax_paginates() {
		Instant t1 = Instant.now().minusSeconds(300).truncatedTo(ChronoUnit.MILLIS);
		Instant t2 = Instant.now().minusSeconds(200).truncatedTo(ChronoUnit.MILLIS);
		Instant t3 = Instant.now().minusSeconds(100).truncatedTo(ChronoUnit.MILLIS);
		Instant t4 = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		persistPlanForProject("OrgA", "ProjA", t1);
		persistPlanForProject("OrgA", "ProjA", t2);
		persistPlanForProject("OrgA", "ProjA", t3);
		persistPlanForProject("OrgA", "ProjA", t4);

		// Desc order: [t4, t3, t2, t1]. Page 1: offset=0, max=2 -> [t4, t3]
		List<TestPlan> page1 = repo.listPlans("OrgA", "ProjA", 0, 2);
		assertThat(page1).hasSize(2);
		assertThat(page1.get(0).createdAt()).isEqualTo(t4);
		assertThat(page1.get(1).createdAt()).isEqualTo(t3);

		// Page 2: offset=2, max=2 -> [t2, t1]
		List<TestPlan> page2 = repo.listPlans("OrgA", "ProjA", 2, 2);
		assertThat(page2).hasSize(2);
		assertThat(page2.get(0).createdAt()).isEqualTo(t2);
		assertThat(page2.get(1).createdAt()).isEqualTo(t1);
	}

	@Test
	void listPlans_resultContainsPlanIdAndProjectId() {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		TestPlan saved = persistPlanForProject("OrgA", "ProjA", now);

		List<TestPlan> result = repo.listPlans("OrgA", "ProjA", 0, 0);
		assertThat(result).hasSize(1);
		assertThat(result.get(0).planID()).isEqualTo(saved.planID());
		assertThat(result.get(0).projectID()).isEqualTo(saved.projectID());
		assertThat(result.get(0).createdAt()).isEqualTo(now);
	}

	@Test
	void reattachNodeToSameParent_maintainsOrder() {
		UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID child1 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		UUID child2 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child2"));
		UUID child3 = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_CASE).name("child3"));

		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeLast(root, child2);
		repo.attachChildNodeLast(root, child3);

		repo.detachChildNode(root, child2);
		repo.attachChildNodeLast(root, child2);

		List<UUID> children = repo.getNodeChildren(root).toList();
		assertThat(children).containsExactly(child1, child3, child2);
	}

}