package org.azertio.core.persistence;

import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestPlanNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Formats a {@link TestPlan} as a hierarchical JSON representation,
 * where each node contains a "nodes" array with its direct children.
 */
public class TestPlanHierarchyFormatter {

    public interface Appender {
        void append(String string) throws IOException;
    }

    private final TestPlanRepository repository;
    private final int maxDepth;

    /** No depth limit. */
    public TestPlanHierarchyFormatter(TestPlanRepository repository) {
        this(repository, -1);
    }

    /**
     * @param maxDepth maximum number of node levels to render; -1 means unlimited.
     */
    public TestPlanHierarchyFormatter(TestPlanRepository repository, int maxDepth) {
        this.repository = repository;
        this.maxDepth = maxDepth;
    }

    public void format(TestPlan testPlan, Appender appender) throws IOException {
        appender.append("{\n");
        appender.append("  \"planID\": " + quoted(testPlan.planID()) + ",\n");
        appender.append("  \"projectID\": " + quoted(testPlan.projectID()) + ",\n");
        appender.append("  \"createdAt\": " + quoted(testPlan.createdAt()) + ",\n");
        appender.append("  \"nodes\": [\n");
        formatNode(testPlan.planNodeRoot(), appender, 2, 0);
        appender.append("\n  ]\n}\n");
    }

    /** Format the tree starting from the given node (depth counter resets to 0 at this node). */
    public void formatFromNode(TestPlanNode node, Appender appender) throws IOException {
        formatNode(node.nodeID(), appender, 0, 0);
        appender.append("\n");
    }

    private void formatNode(UUID nodeID, Appender appender, int indent, int depth) throws IOException {
        String pad = "  ".repeat(indent);
        TestPlanNode node = repository.getNodeData(nodeID).orElseThrow();
        appender.append(pad + "{\n");
        appendField(appender, pad, "nodeID", node.nodeID());
        appendField(appender, pad, "nodeType", node.nodeType());
        appendField(appender, pad, "name", node.name());
        appendField(appender, pad, "identifier", node.identifier());
        appendField(appender, pad, "source", node.source());
        appendField(appender, pad, "keyword", node.keyword());
        appendField(appender, pad, "language", node.language());
        appendField(appender, pad, "validationStatus", node.validationStatus());
        appendField(appender, pad, "validationMessage", node.validationMessage());
        appendField(appender, pad, "hasIssues", node.hasIssues());
        appendTags(appender, pad, node);
        appendProperties(appender, pad, node);
        appender.append(pad + "  \"nodes\": ");
        formatChildren(nodeID, appender, indent + 1, depth + 1);
        appender.append("\n" + pad + "}");
    }

    private void formatChildren(UUID parentID, Appender appender, int indent, int depth) throws IOException {
        if (maxDepth >= 0 && depth >= maxDepth) {
            appender.append("[]");
            return;
        }
        List<UUID> children = repository.getNodeChildren(parentID).toList();
        if (children.isEmpty()) {
            appender.append("[]");
            return;
        }
        appender.append("[\n");
        for (int i = 0; i < children.size(); i++) {
            formatNode(children.get(i), appender, indent + 1, depth);
            if (i < children.size() - 1) appender.append(",");
            appender.append("\n");
        }
        appender.append("  ".repeat(indent) + "]");
    }

    private void appendField(Appender appender, String pad, String key, Object value) throws IOException {
        appender.append(pad + "  " + quoted(key) + ": ");
        if (value == null) {
            appender.append("null");
        } else if (value instanceof Boolean || value instanceof Number) {
            appender.append(String.valueOf(value));
        } else {
            appender.append(quoted(value));
        }
        appender.append(",\n");
    }

    private void appendTags(Appender appender, String pad, TestPlanNode node) throws IOException {
        appender.append(pad + "  \"tags\": [");
        List<String> tags = node.tags() != null ? node.tags().stream().sorted().toList() : List.of();
        for (int i = 0; i < tags.size(); i++) {
            appender.append(quoted(tags.get(i)));
            if (i < tags.size() - 1) appender.append(", ");
        }
        appender.append("],\n");
    }

    private void appendProperties(Appender appender, String pad, TestPlanNode node) throws IOException {
        appender.append(pad + "  \"properties\": {");
        Map<String, String> props = node.properties() != null ? node.properties() : Map.of();
        List<Map.Entry<String, String>> entries = props.entrySet().stream().toList();
        for (int i = 0; i < entries.size(); i++) {
            appender.append(quoted(entries.get(i).getKey()) + ": " + quoted(entries.get(i).getValue()));
            if (i < entries.size() - 1) appender.append(", ");
        }
        appender.append("},\n");
    }

    private String quoted(Object value) {
        if (value == null) return "null";
        return "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
