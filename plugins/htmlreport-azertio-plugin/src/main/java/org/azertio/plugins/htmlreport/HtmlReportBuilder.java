package org.azertio.plugins.htmlreport;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.azertio.core.Clock;
import org.azertio.core.contributors.ReportBuilder;
import org.azertio.core.execution.ExecutionResult;
import org.azertio.core.execution.TestExecution;
import org.azertio.core.execution.TestExecutionNode;
import org.azertio.core.persistence.TestExecutionRepository;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.NodeType;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestPlanNode;
import org.azertio.core.testplan.TestProject;
import org.azertio.core.util.FileUtil;
import org.azertio.core.util.Log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Extension(
    name = "HTML Report Builder",
    extensionPointVersion = "1.0"
)
public class HtmlReportBuilder implements ReportBuilder {

    private static final Log log = Log.of("plugins.htmlreport");

    @Inject Config config;
    @Inject Clock clock;
    @Inject TestExecutionRepository executionRepository;
    @Inject TestPlanRepository planRepository;

    @Override
    public void buildReport(UUID executionID) {

        if (!config.get("htmlreport.enabled", Boolean.class).orElse(true)) {
            return;
        }

        Path outputDir = config.get("htmlreport.outputDir", Path::of)
            .orElse(Path.of(".azertio", "reports"));
        boolean includePassedSteps = config.get("htmlreport.includePassedSteps", Boolean.class).orElse(true);
        String filePattern = config.getString("htmlreport.outputFile").orElse("%Y%m%d-%h%M%s.html");

        TestExecution execution = executionRepository.getExecution(executionID)
            .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionID));

        Instant reportTime = execution.executedAt() != null ? execution.executedAt() : clock.now();
        Path outputFile = outputDir.resolve(FileUtil.resolvePattern(filePattern, () -> reportTime));

        log.info("Generating HTML report for execution {} -> {}", executionID, outputFile);
        TestPlan plan = planRepository.getPlan(execution.planID())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + execution.planID()));

        var projectOpt = planRepository.getProject(plan.projectID());
        String title = config.getString("htmlreport.title")
            .or(() -> projectOpt.map(TestProject::name))
            .orElse("Azertio Report");
        String organization = projectOpt.map(TestProject::organization).orElse(null);
        String logoPath = config.getString("htmlreport.logoPath").orElse(null);
        String accentColor = config.getString("htmlreport.accentColor").orElse(null);
        String footerText = config.getString("htmlreport.footer").orElse(null);

        List<UUID> suiteIDs;
        try (var rootChildren = planRepository.getNodeChildren(plan.planNodeRoot())) {
            suiteIDs = rootChildren
                .filter(id -> planRepository.getNodeData(id)
                    .map(n -> n.nodeType() == NodeType.TEST_SUITE).orElse(false))
                .toList();
        }
        List<String> suiteNames = suiteIDs.stream()
            .flatMap(id -> planRepository.getNodeData(id).stream())
            .map(n -> n.name() != null ? n.name() : "")
            .filter(s -> !s.isEmpty())
            .toList();

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create output directory: " + outputDir, e);
        }

        int[] tcCounts = {
            execution.testPassedCount() != null ? execution.testPassedCount() : 0,
            execution.testFailedCount() != null ? execution.testFailedCount() : 0,
            execution.testErrorCount()  != null ? execution.testErrorCount()  : 0,
            plan.testCaseCount()
        };
        int[] suiteCounts = levelCounts(suiteIDs, executionID);
        int[] featCounts  = featureCounts(suiteIDs, executionID);

        HtmlRenderer renderer = new HtmlRenderer(accentColor, footerText, logoPath);
        renderer.startDocument(title, organization, suiteNames, execution, plan);
        renderer.addStatistics(suiteCounts, featCounts, tcCounts);
        renderer.startResults();
        renderChildren(plan.planNodeRoot(), executionID, renderer, includePassedSteps);
        renderer.endResults();
        renderer.endDocument();

        try {
            Files.writeString(outputFile, renderer.build(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        log.info("HTML report written to {}", outputFile);
    }

    private void renderChildren(UUID parentID, UUID executionID, HtmlRenderer renderer, boolean includePassedSteps) {
        try (var children = planRepository.getNodeChildren(parentID)) {
            children.forEach(childID ->
                renderNode(childID, executionID, renderer, includePassedSteps));
        }
    }

    private void renderNode(UUID planNodeID, UUID executionID, HtmlRenderer renderer, boolean includePassedSteps) {
        TestPlanNode node = planRepository.getNodeData(planNodeID).orElse(null);
        if (node == null) return;

        TestExecutionNode execNode = executionRepository.getExecutionNode(executionID, planNodeID).orElse(null);

        switch (node.nodeType()) {
            case TEST_PLAN       -> { /* rendered as cover */ }
            case TEST_SUITE -> {
                renderer.startSuite(node, execNode);
                renderChildren(planNodeID, executionID, renderer, includePassedSteps);
                renderer.endSuite();
            }
            case TEST_FEATURE -> {
                renderer.startFeature(node, execNode);
                renderChildren(planNodeID, executionID, renderer, includePassedSteps);
                renderer.endFeature();
            }
            case TEST_CASE -> {
                renderer.startTestCase(node, execNode);
                renderChildren(planNodeID, executionID, renderer, includePassedSteps);
                renderer.endTestCase();
            }
            case STEP_AGGREGATOR -> renderChildren(planNodeID, executionID, renderer, includePassedSteps);
            case STEP -> {
                if (includePassedSteps || !isPassed(execNode)) {
                    renderer.renderStep(node, execNode);
                }
            }
            case VIRTUAL_STEP -> {
                if (includePassedSteps || !isPassed(execNode)) {
                    renderer.renderVirtualStep(node);
                }
            }
        }
    }

    private boolean isPassed(TestExecutionNode execNode) {
        return execNode != null && execNode.result() == ExecutionResult.PASSED;
    }

    private int[] levelCounts(List<UUID> nodeIDs, UUID executionID) {
        int[] c = {0, 0, 0, nodeIDs.size()};
        for (UUID id : nodeIDs) {
            executionRepository.getExecutionNode(executionID, id).ifPresent(n -> {
                if (n.result() != null) switch (n.result()) {
                    case PASSED -> c[0]++;
                    case FAILED -> c[1]++;
                    case ERROR  -> c[2]++;
                    default     -> {}
                }
            });
        }
        return c;
    }

    private int[] featureCounts(List<UUID> suiteIDs, UUID executionID) {
        List<UUID> featIDs = new ArrayList<>();
        for (UUID suiteID : suiteIDs) {
            try (var children = planRepository.getNodeChildren(suiteID)) {
                children.filter(id -> planRepository.getNodeData(id)
                        .map(n -> n.nodeType() == NodeType.TEST_FEATURE).orElse(false))
                    .forEach(featIDs::add);
            }
        }
        return levelCounts(featIDs, executionID);
    }

}