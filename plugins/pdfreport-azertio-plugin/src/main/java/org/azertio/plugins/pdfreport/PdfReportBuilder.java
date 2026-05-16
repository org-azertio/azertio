package org.azertio.plugins.pdfreport;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
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
import org.azertio.core.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Extension(
    name = "PDF Report Builder",
    extensionPointVersion = "1.0"
)
public class PdfReportBuilder implements ReportBuilder {

    private static final Log log = Log.of("plugins.pdfreport");

    @Inject Config config;
    @Inject TestExecutionRepository executionRepository;
    @Inject TestPlanRepository planRepository;

    @Override
    public void buildReport(UUID executionID) {

        if (!config.get("pdfreport.enabled", Boolean.class).orElse(true)) {
            return;
        }

        Path outputDir = config.get("pdfreport.outputDir", Path::of)
            .orElse(Path.of(".azertio", "reports"));
        boolean includePassedSteps = config.get("pdfreport.includePassedSteps", Boolean.class).orElse(true);
        String filePattern = config.getString("pdfreport.outputFile").orElse("%Y%m%d-%h%M%s.pdf");
        PageBreakLevel pageBreak = config.getString("pdfreport.pageBreak")
            .map(s -> PageBreakLevel.valueOf(s.toUpperCase().replace('-', '_')))
            .orElse(PageBreakLevel.NONE);

        TestExecution execution = executionRepository.getExecution(executionID)
            .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionID));

        Instant reportTime = execution.executedAt() != null ? execution.executedAt() : Instant.now();
        Path outputFile = outputDir.resolve(resolvePattern(filePattern, reportTime));

        log.info("Generating PDF report for execution {} -> {}", executionID, outputFile);
        TestPlan plan = planRepository.getPlan(execution.planID())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + execution.planID()));

        var projectOpt = planRepository.getProject(plan.projectID());
        String title = config.getString("pdfreport.title")
            .or(() -> projectOpt.map(TestProject::name))
            .orElse("Azertio Report");
        String organization = projectOpt.map(TestProject::organization).orElse(null);
        String logoPath = config.getString("pdfreport.logoPath").orElse(null);
        float[] accentColor = config.getString("pdfreport.accentColor")
            .map(PdfReportBuilder::parseHexColor)
            .orElse(null);
        String footerText = config.getString("pdfreport.footer").orElse(null);

        List<UUID>   suiteIDs;
        List<String> suiteNames;
        try (var rootChildren = planRepository.getNodeChildren(plan.planNodeRoot())) {
            suiteIDs = rootChildren
                .filter(id -> planRepository.getNodeData(id)
                    .map(n -> n.nodeType() == NodeType.TEST_SUITE).orElse(false))
                .toList();
        }
        suiteNames = suiteIDs.stream()
            .flatMap(id -> planRepository.getNodeData(id).stream())
            .map(n -> n.name() != null ? n.name() : "")
            .filter(s -> !s.isEmpty())
            .toList();

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create output directory: " + outputDir, e);
        }

        try (
            PdfRenderer renderer = new PdfRenderer(accentColor, footerText);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile.toFile()))
        ) {
            int[] tcCounts = {
                execution.testPassedCount() != null ? execution.testPassedCount() : 0,
                execution.testFailedCount() != null ? execution.testFailedCount() : 0,
                execution.testErrorCount()  != null ? execution.testErrorCount()  : 0,
                plan.testCaseCount()
            };
            int[] suiteCounts = levelCounts(suiteIDs, executionID);
            int[] featCounts  = featureCounts(suiteIDs, executionID);

            renderer.addCoverPage(title, organization, suiteNames, execution, plan, logoPath);
            renderer.addStatisticsPage(suiteCounts, featCounts, tcCounts);
            renderChildren(plan.planNodeRoot(), executionID, renderer, includePassedSteps, pageBreak);
            renderer.save(out);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF report for execution " + executionID, e);
        }

        log.info("PDF report written to {}", outputFile);
    }

    private void renderChildren(
        UUID parentID,
        UUID executionID,
        PdfRenderer renderer,
        boolean includePassedSteps,
        PageBreakLevel pageBreak
    ) {
        try (var children = planRepository.getNodeChildren(parentID)) {
            children.forEach(childID ->
                renderNode(childID, executionID, renderer, includePassedSteps, pageBreak));
        }
    }

    private void renderNode(
        UUID planNodeID,
        UUID executionID,
        PdfRenderer renderer,
        boolean includePassedSteps,
        PageBreakLevel pageBreak
    ) {
        TestPlanNode node = planRepository.getNodeData(planNodeID).orElse(null);
        if (node == null) return;

        TestExecutionNode execNode = executionRepository.getExecutionNode(executionID, planNodeID).orElse(null);

        try {
            switch (node.nodeType()) {
                case TEST_PLAN       -> { /* rendered as cover page */ }
                case TEST_SUITE -> {
                    if (pageBreak == PageBreakLevel.SUITE && !renderer.isAtPageTop()) renderer.breakPage();
                    renderer.renderSuiteHeader(node, execNode);
                }
                case TEST_FEATURE -> {
                    if (pageBreak == PageBreakLevel.FEATURE && !renderer.isAtPageTop()) renderer.breakPage();
                    renderer.renderFeatureHeader(node, execNode);
                }
                case TEST_CASE -> {
                    if (pageBreak == PageBreakLevel.TEST_CASE && !renderer.isAtPageTop()) renderer.breakPage();
                    renderer.renderTestCase(node, execNode);
                }
                case STEP_AGGREGATOR -> { /* transparent container */ }
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        renderChildren(planNodeID, executionID, renderer, includePassedSteps, pageBreak);
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

    private static float[] parseHexColor(String hex) {
        String h = hex.trim().replace("#", "");
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return new float[]{r / 255f, g / 255f, b / 255f};
    }

    private String resolvePattern(String pattern, Instant instant) {
        ZonedDateTime dt = instant.atZone(ZoneId.systemDefault());
        return pattern
            .replace("%Y", String.format("%04d", dt.getYear()))
            .replace("%m", String.format("%02d", dt.getMonthValue()))
            .replace("%d", String.format("%02d", dt.getDayOfMonth()))
            .replace("%h", String.format("%02d", dt.getHour()))
            .replace("%M", String.format("%02d", dt.getMinute()))
            .replace("%s", String.format("%02d", dt.getSecond()));
    }
}