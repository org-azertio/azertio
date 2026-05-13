package org.azertio.cli.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.cli.MainCommand;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioFile;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.TestPlan;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BrowseCommandTest {

    static final String ENV_PATH = "target/.azertio-browse";

    static String planID;
    static String rootNodeID;

    static final String[] BASE_ARGS = {
        "-f", "src/test/resources/azertio.yaml",
        "-D" + AzertioConfig.ENV_PATH + "=" + ENV_PATH,
        "-D" + AzertioConfig.PERSISTENCE_MODE + "=" + AzertioConfig.PERSISTENCE_MODE_FILE
    };

    @BeforeAll
    static void setup() throws Exception {
        new CommandLine(new MainCommand()).execute(
            "install",
            "-f", "src/test/resources/azertio.yaml",
            "-D" + AzertioConfig.ENV_PATH + "=" + ENV_PATH
        );

        try (var reader = new FileReader("src/test/resources/azertio.yaml")) {
            AzertioFile file = AzertioFile.read(reader);
            Map<String, String> params = Map.of(
                AzertioConfig.ENV_PATH, ENV_PATH,
                AzertioConfig.RESOURCE_PATH, "src/test/resources/test-features",
                AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_FILE
            );
            var context = file.createContext(
                Config.ofMap(params),
                List.of("suiteA")
            );
            AzertioRuntime runtime = new AzertioRuntime(context.configuration());
            TestPlan testPlan = runtime.buildTestPlan(context);
            planID = testPlan.planID().toString();

            // grab the first child of root to use in --node tests
            TestPlanRepository repository = runtime.getRepository(TestPlanRepository.class);
            rootNodeID = repository.getNodeChildren(testPlan.planNodeRoot())
                .findFirst()
                .map(UUID::toString)
                .orElse(testPlan.planNodeRoot().toString());
        }
    }

    @Test
    void showHelp() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("browse", "--help", "--plan", UUID.randomUUID().toString())
        );
        assertEquals(0, exitCode);
    }

    @Test
    void browseShowsPlanSummaryWithoutNodes() {
        var out = captureStdout(args("browse", "--plan", planID));
        assertEquals(0, out.exitCode);
        assertTrue(out.text.contains("Plan ID:"));
        assertTrue(out.text.contains("Project ID:"));
        assertTrue(out.text.contains("Created at:"));
        assertFalse(out.text.contains("nodes"), "Default output should not contain nodes");
    }

    @Test
    void browseJsonShowsHierarchicalNodes() {
        var out = captureStdout(args("browse", "--plan", planID, "--json"));
        assertEquals(0, out.exitCode);
        assertTrue(out.text.contains("\"planID\""));
        assertTrue(out.text.contains("\"nodes\""));
    }

    @Test
    void browseJsonWithDepthLimitsTree() {
        var fullOut = captureStdout(args("browse", "--plan", planID, "--json"));
        var depthOut = captureStdout(args("browse", "--plan", planID, "--json", "--depth", "1"));
        assertEquals(0, fullOut.exitCode);
        assertEquals(0, depthOut.exitCode);
        assertTrue(depthOut.text.length() < fullOut.text.length(),
            "--depth 1 should produce less output than unlimited");
    }

    @Test
    void browseNodeShowsHierarchicalText() {
        var out = captureStdout(args("browse", "--node", rootNodeID));
        assertEquals(0, out.exitCode);
        assertTrue(out.text.contains("[TEST_SUITE]") || out.text.contains("[TEST_PLAN]")
            || out.text.contains("[TEST_FEATURE]") || out.text.contains("[TEST_CASE]"));
    }

    @Test
    void browseNodeJsonShowsSubtree() {
        var out = captureStdout(args("browse", "--node", rootNodeID, "--json"));
        assertEquals(0, out.exitCode);
        assertTrue(out.text.contains("\"nodeID\""));
        assertTrue(out.text.contains("\"nodes\""));
    }

    @Test
    void browseNodeJsonWithDepthLimitsSubtree() {
        var fullOut = captureStdout(args("browse", "--node", rootNodeID, "--json"));
        var depthOut = captureStdout(args("browse", "--node", rootNodeID, "--json", "--depth", "1"));
        assertEquals(0, fullOut.exitCode);
        assertEquals(0, depthOut.exitCode);
        assertTrue(depthOut.text.length() < fullOut.text.length(),
            "--depth 1 should produce less output than unlimited");
    }

    @Test
    void browseNonExistentPlan() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("browse", "--plan", UUID.randomUUID().toString())
        );
        assertEquals(1, exitCode);
    }

    @Test
    void browseNonExistentNode() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("browse", "--node", UUID.randomUUID().toString())
        );
        assertEquals(1, exitCode);
    }

    // --- helpers ---

    static String[] args(String... extra) {
        List<String> all = new ArrayList<>(Arrays.asList(extra));
        all.addAll(Arrays.asList(BASE_ARGS));
        return all.toArray(String[]::new);
    }

    record CapturedOutput(int exitCode, String text) {}

    static CapturedOutput captureStdout(String... args) {
        var stdout = new ByteArrayOutputStream();
        var stderr = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(new MainCommand());
        commandLine.setOut(new PrintWriter(new OutputStreamWriter(stdout, StandardCharsets.UTF_8), true));
        commandLine.setErr(new PrintWriter(new OutputStreamWriter(stderr, StandardCharsets.UTF_8), true));
        int code = commandLine.execute(args);
        return new CapturedOutput(code, stdout.toString(StandardCharsets.UTF_8));
    }
}
