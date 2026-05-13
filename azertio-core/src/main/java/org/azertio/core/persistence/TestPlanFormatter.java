package org.azertio.core.persistence;

import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestPlanNode;
import org.azertio.core.testplan.ValidationStatus;

import java.io.IOException;
import java.util.UUID;

/**
 * Formats a {@link TestPlan} and its nodes into human-readable text output.
 */
public class TestPlanFormatter {

    public interface Appender {
        void append(String string) throws IOException;
    }

    private final TestPlanRepository repository;
    private final int maxDepth;

    public TestPlanFormatter(TestPlanRepository repository) {
        this(repository, -1);
    }

    public TestPlanFormatter(TestPlanRepository repository, int maxDepth) {
        this.repository = repository;
        this.maxDepth = maxDepth;
    }

    public void format(TestPlan testPlan, Appender appender) throws IOException {
        appender.append("Plan ID:    " + testPlan.planID() + "\n");
        appender.append("Project ID: " + testPlan.projectID() + "\n");
        appender.append("Created at: " + testPlan.createdAt() + "\n");
        appender.append("\n");
        formatNode(testPlan.planNodeRoot(), appender, 0, 0);
    }

    public void formatFromNode(TestPlanNode node, Appender appender) throws IOException {
        formatNode(node.nodeID(), appender, 0, 0);
    }

    private void formatNode(UUID nodeID, Appender appender, int indent, int depth) throws IOException {
        TestPlanNode node = repository.getNodeData(nodeID).orElseThrow();
        appender.append("  ".repeat(indent));
        appender.append("[");
        appender.append(String.valueOf(node.nodeType()));
        appender.append("] ");
        if (node.identifier() != null) {
            appender.append("(");
            appender.append(node.identifier());
            appender.append(") ");
        }
        appender.append(node.toString());
        if (node.source() != null) {
            appender.append("  <");
            appender.append(node.source());
            appender.append(">");
        }
        if (node.validationStatus() == ValidationStatus.ERROR) {
            appender.append("  !! ");
            appender.append(node.validationMessage());
        }
        appender.append("\n");
        if (maxDepth < 0 || depth < maxDepth) {
            for (UUID childID : repository.getNodeChildren(nodeID).toList()) {
                formatNode(childID, appender, indent + 1, depth + 1);
            }
        }
    }
}