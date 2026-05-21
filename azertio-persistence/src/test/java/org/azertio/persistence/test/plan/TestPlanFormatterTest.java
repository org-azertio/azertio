package org.azertio.persistence.test.plan;

import com.zaxxer.hikari.HikariDataSource;
import org.azertio.core.persistence.TestPlanFormatter;
import org.azertio.core.persistence.TestPlanHierarchyFormatter;
import org.azertio.core.testplan.NodeType;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestPlanNode;
import org.azertio.core.testplan.TestProject;
import org.azertio.core.testplan.ValidationStatus;
import org.azertio.persistence.DataSourceProvider;
import org.azertio.persistence.plan.JooqPlanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestPlanFormatterTest {

    @TempDir
    Path tempDir;

    private JooqPlanRepository repo;
    private HikariDataSource dataSource;

    @BeforeEach
    void setUp() {
        DataSourceProvider provider = DataSourceProvider.h2fileLocal(tempDir.resolve("testdb"));
        dataSource = (HikariDataSource) provider.obtainDataSource();
        repo = new JooqPlanRepository(dataSource, provider.dialect());
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    private TestPlan buildPlan() {
        UUID projectId = repo.persistProject(new TestProject("MyProject", "desc", "Org", java.util.List.of()));
        UUID rootId = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("Root"));
        UUID caseId = repo.persistNode(new TestPlanNode()
            .nodeType(NodeType.TEST_CASE)
            .name("Test Case 1")
            .source("test.feature")
            .identifier("TC-001"));
        UUID errorCaseId = repo.persistNode(new TestPlanNode()
            .nodeType(NodeType.TEST_CASE)
            .name("Bad Case"));
        repo.attachChildNodeLast(rootId, caseId);
        repo.attachChildNodeLast(rootId, errorCaseId);
        repo.setNodeValidation(errorCaseId, ValidationStatus.ERROR, "No steps found");

        return repo.persistPlan(new TestPlan(null, projectId, Instant.now(), "hash1", "hash2", rootId, 2, null));
    }

    // ── TestPlanFormatter ─────────────────────────────────────────────────────

    @Test
    void format_containsPlanMetadata() throws IOException {
        TestPlan plan = buildPlan();
        var sb = new StringBuilder();
        new TestPlanFormatter(repo).format(plan, sb::append);
        assertThat(sb.toString())
            .contains("Plan ID:")
            .contains("Project ID:")
            .contains("Created at:");
    }

    @Test
    void format_rendersNodeTree() throws IOException {
        TestPlan plan = buildPlan();
        var sb = new StringBuilder();
        new TestPlanFormatter(repo).format(plan, sb::append);
        assertThat(sb.toString())
            .contains("[TEST_PLAN]")
            .contains("[TEST_CASE]")
            .contains("(TC-001)")
            .contains("<test.feature>");
    }

    @Test
    void format_showsValidationError() throws IOException {
        TestPlan plan = buildPlan();
        var sb = new StringBuilder();
        new TestPlanFormatter(repo).format(plan, sb::append);
        assertThat(sb.toString()).contains("!! No steps found");
    }

    @Test
    void format_respectsMaxDepth() throws IOException {
        TestPlan plan = buildPlan();
        var sb = new StringBuilder();
        new TestPlanFormatter(repo, 0).format(plan, sb::append);
        assertThat(sb.toString())
            .contains("[TEST_PLAN]")
            .doesNotContain("[TEST_CASE]");
    }

    @Test
    void formatFromNode_formatsSubtree() throws IOException {
        buildPlan();
        UUID suiteId = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_SUITE).name("My Suite"));
        TestPlanNode suite = repo.getNodeData(suiteId).orElseThrow();
        var sb = new StringBuilder();
        new TestPlanFormatter(repo).formatFromNode(suite, sb::append);
        assertThat(sb.toString()).contains("[TEST_SUITE]");
    }

    // ── TestPlanHierarchyFormatter ────────────────────────────────────────────

    @Test
    void hierarchyFormat_producesJsonWithPlanFields() throws IOException {
        TestPlan plan = buildPlan();
        var sb = new StringBuilder();
        new TestPlanHierarchyFormatter(repo).format(plan, sb::append);
        String json = sb.toString();
        assertThat(json)
            .contains("\"planID\":")
            .contains("\"projectID\":")
            .contains("\"nodes\":");
    }

    @Test
    void hierarchyFormat_rendersChildNodes() throws IOException {
        TestPlan plan = buildPlan();
        var sb = new StringBuilder();
        new TestPlanHierarchyFormatter(repo).format(plan, sb::append);
        assertThat(sb.toString()).contains("\"nodeType\": \"TEST_CASE\"");
    }

    @Test
    void hierarchyFormat_withMaxDepth_truncatesChildren() throws IOException {
        TestPlan plan = buildPlan();
        var sb = new StringBuilder();
        new TestPlanHierarchyFormatter(repo, 0).format(plan, sb::append);
        assertThat(sb.toString())
            .contains("\"planID\":")
            .doesNotContain("TEST_CASE");
    }

    @Test
    void hierarchyFormatFromNode_rendersNode() throws IOException {
        UUID nodeId = repo.persistNode(new TestPlanNode()
            .nodeType(NodeType.TEST_SUITE)
            .name("Suite A")
            .tags(new java.util.HashSet<>(java.util.List.of("smoke")))
            .properties(new java.util.TreeMap<>(java.util.Map.of("key", "val"))));
        TestPlanNode node = repo.getNodeData(nodeId).orElseThrow();
        var sb = new StringBuilder();
        new TestPlanHierarchyFormatter(repo).formatFromNode(node, sb::append);
        assertThat(sb.toString())
            .contains("\"nodeID\":")
            .contains("\"nodeType\": \"TEST_SUITE\"");
    }
}