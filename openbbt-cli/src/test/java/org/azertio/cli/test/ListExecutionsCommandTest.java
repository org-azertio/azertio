package org.azertio.cli.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.cli.MainCommand;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioFile;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.execution.ExecutionResult;
import org.azertio.core.execution.TestExecution;
import org.azertio.core.persistence.TestExecutionRepository;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.NodeType;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestPlanNode;
import org.azertio.core.testplan.TestProject;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListExecutionsCommandTest {

    static final String ENV_PATH = "target/.azertio-listexecs";

    static String planId;
    // execution IDs ordered oldest -> newest: [0]=oldest, [2]=newest
    static final List<String> executionIds = new ArrayList<>();

    static final String[] BASE_ARGS = {
        "-f", "src/test/resources/azertio.yaml",
        "-D" + AzertioConfig.ENV_PATH + "=" + ENV_PATH,
        "-D" + AzertioConfig.PERSISTENCE_MODE + "=" + AzertioConfig.PERSISTENCE_MODE_FILE
    };

    @BeforeAll
    static void setup() throws Exception {
        deleteDirectory(Path.of(ENV_PATH));
        new CommandLine(new MainCommand()).execute(
            "install",
            "-f", "src/test/resources/azertio.yaml",
            "-D" + AzertioConfig.ENV_PATH + "=" + ENV_PATH
        );

        try (var reader = new FileReader("src/test/resources/azertio.yaml")) {
            AzertioFile file = AzertioFile.read(reader);
            Map<String, String> params = Map.of(
                AzertioConfig.ENV_PATH, ENV_PATH,
                AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_FILE
            );
            var context = file.createContext(
                Config.ofMap(params), List.of()
            );
            AzertioRuntime runtime = new AzertioRuntime(context.configuration());
            TestPlanRepository planRepo = runtime.getRepository(TestPlanRepository.class);
            TestExecutionRepository execRepo = runtime.getRepository(TestExecutionRepository.class);

            // Create a plan with a root node
            UUID root = planRepo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
            UUID projectId = planRepo.persistProject(new TestProject("P", "desc", "Org", List.of()));
            TestPlan plan = planRepo.persistPlan(new TestPlan(null, projectId, Instant.now(), "rh", "ch", root, 0, null));
            planId = plan.planID().toString();

            // 3 executions with distinct times and results (oldest first in list)
            ExecutionResult[] results = {ExecutionResult.PASSED, ExecutionResult.FAILED, ExecutionResult.ERROR};
            for (int i = 0; i < 3; i++) {
                Instant executedAt = Instant.now().minusSeconds(200L - i * 100);
                TestExecution ex = execRepo.newExecution(plan.planID(), executedAt, null);
                UUID rootExecNodeID = execRepo.newExecutionNode(ex.executionID(), root);
                execRepo.updateExecutionNodeFinish(rootExecNodeID, results[i], executedAt.plusSeconds(1));
                executionIds.add(ex.executionID().toString());
            }

            // A second plan with its own execution — must never appear in results
            UUID root2 = planRepo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root2"));
            TestPlan plan2 = planRepo.persistPlan(new TestPlan(null, projectId, Instant.now(), "rh2", "ch2", root2, 0, null));
            TestExecution otherEx = execRepo.newExecution(plan2.planID(), Instant.now(), null);
            UUID otherExecNode = execRepo.newExecutionNode(otherEx.executionID(), root2);
            execRepo.updateExecutionNodeFinish(otherExecNode, ExecutionResult.PASSED, Instant.now());
        }
    }

    @Test
    void showHelp() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("list-executions", "--help", "--plan-id", planId)
        );
        assertEquals(0, exitCode);
    }

    @Test
    void listExecutionsOutputsJsonArrayWithExpectedFields() {
        var out = captureStdout(args("list-executions", "--json", "--plan-id", planId));
        assertEquals(0, out.exitCode());
        assertTrue(out.text().contains("\"executionId\""));
        assertTrue(out.text().contains("\"executedAt\""));
        assertTrue(out.text().contains("\"planId\""));
    }

    @Test
    void listExecutionsReturnsAllThreeExecutions() {
        var out = captureStdout(args("list-executions", "--json", "--plan-id", planId));
        assertEquals(0, out.exitCode());
        assertEquals(3, countOccurrences(out.text(), "\"executionId\""));
    }

    @Test
    void listExecutionsOrderedByExecutedAtDescending() {
        var out = captureStdout(args("list-executions", "--json", "--plan-id", planId));
        assertEquals(0, out.exitCode());
        // [0]=oldest, [2]=newest — desc order means [2] appears first
        int pos0 = out.text().indexOf(executionIds.get(0));
        int pos1 = out.text().indexOf(executionIds.get(1));
        int pos2 = out.text().indexOf(executionIds.get(2));
        assertTrue(pos2 < pos1 && pos1 < pos0, "Newest execution should appear first");
    }

    @Test
    void listExecutionsWithMaxLimitsResults() {
        var out = captureStdout(args("list-executions", "--json", "--plan-id", planId, "--max", "2"));
        assertEquals(0, out.exitCode());
        assertEquals(2, countOccurrences(out.text(), "\"executionId\""));
    }

    @Test
    void listExecutionsWithOffsetSkipsNewest() {
        // Desc order: [newest, middle, oldest]. Offset=1 skips newest -> [middle, oldest]
        var out = captureStdout(args("list-executions", "--json", "--plan-id", planId, "--offset", "1"));
        assertEquals(0, out.exitCode());
        assertEquals(2, countOccurrences(out.text(), "\"executionId\""));
        assertFalse(out.text().contains(executionIds.get(2)), "Newest should be skipped");
        assertTrue(out.text().contains(executionIds.get(1)));
        assertTrue(out.text().contains(executionIds.get(0)));
    }

    @Test
    void listExecutionsWithOffsetAndMaxPaginates() {
        // Desc: [newest, middle, oldest]. offset=1, max=1 -> [middle]
        var out = captureStdout(args("list-executions", "--json", "--plan-id", planId, "--offset", "1", "--max", "1"));
        assertEquals(0, out.exitCode());
        assertEquals(1, countOccurrences(out.text(), "\"executionId\""));
        assertTrue(out.text().contains(executionIds.get(1)));
        assertFalse(out.text().contains(executionIds.get(2)));
        assertFalse(out.text().contains(executionIds.get(0)));
    }

    @Test
    void listExecutionsReturnsEmptyArrayForUnknownPlan() {
        var out = captureStdout(args("list-executions", "--json", "--plan-id", UUID.randomUUID().toString()));
        assertEquals(0, out.exitCode());
        assertEquals("[]", out.text().trim());
    }

    @Test
    void listExecutionsDoesNotIncludeExecutionsFromOtherPlans() {
        var out = captureStdout(args("list-executions", "--json", "--plan-id", planId));
        assertEquals(0, out.exitCode());
        assertEquals(3, countOccurrences(out.text(), "\"executionId\""),
            "Must not include executions from other plans");
    }

    @Test
    void plainOutputContainsExecutionIdDateAndResult() {
        // Without --json: each line is "<id> <date> <result>"
        var out = captureStdout(args("list-executions", "--plan-id", planId));
        assertEquals(0, out.exitCode());
        String[] lines = out.text().trim().split("\\n");
        assertEquals(3, lines.length);
        // Newest first -> result is ERROR (index 2 in our setup)
        assertTrue(lines[0].contains("ERROR"),  "First line (newest) should show ERROR");
        assertTrue(lines[1].contains("FAILED"), "Second line should show FAILED");
        assertTrue(lines[2].contains("PASSED"), "Third line (oldest) should show PASSED");
    }

    // --- helpers ---

    static String[] args(String... extra) {
        List<String> all = new ArrayList<>(Arrays.asList(extra));
        all.addAll(Arrays.asList(BASE_ARGS));
        return all.toArray(String[]::new);
    }

    record CapturedOutput(int exitCode, String text) {}

    static CapturedOutput captureStdout(String... args) {
        var buffer = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(new MainCommand());
        commandLine.setOut(new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8), true));
        commandLine.setErr(new PrintWriter(new OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8), true));
        int code = commandLine.execute(args);
        return new CapturedOutput(code, buffer.toString(StandardCharsets.UTF_8));
    }

    static int countOccurrences(String text, String token) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) { count++; idx += token.length(); }
        return count;
    }

    static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }
}
