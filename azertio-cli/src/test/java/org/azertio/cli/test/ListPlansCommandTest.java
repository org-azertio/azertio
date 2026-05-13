package org.azertio.cli.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.cli.MainCommand;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioFile;
import org.azertio.core.AzertioRuntime;
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

class ListPlansCommandTest {

    static final String ORGANIZATION = "ListPlansTestOrg";
    static final String PROJECT = "ListPlansTestProject";
    static final String OTHER_PROJECT = "OtherProject";

    // Inserted plan IDs ordered oldest -> newest: [0]=oldest, [2]=newest
    static final List<String> planIds = new ArrayList<>();

    static final String ENV_PATH = "target/.azertio-listplans";

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
            TestPlanRepository repo = runtime.getRepository(TestPlanRepository.class);

            TestProject project = new TestProject(PROJECT, "desc", ORGANIZATION, List.of());
            UUID projectId = repo.persistProject(project);

            // Insert 3 plans with distinct creation times (oldest -> newest)
            for (int i = 0; i < 3; i++) {
                UUID root = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root" + i));
                Instant createdAt = Instant.now().minusSeconds(200L - i * 100);
                TestPlan plan = repo.persistPlan(new TestPlan(null, projectId, createdAt, "h" + i, "c" + i, root, 0, null));
                planIds.add(plan.planID().toString());
            }

            // A plan under a different project (should never appear in results)
            TestProject other = new TestProject(OTHER_PROJECT, "desc", ORGANIZATION, List.of());
            UUID otherId = repo.persistProject(other);
            UUID otherRoot = repo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("otherRoot"));
            repo.persistPlan(new TestPlan(null, otherId, Instant.now(), "hx", "cx", otherRoot, 0, null));
        }
    }

    @Test
    void showHelp() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("list-plans", "--help", "--organization", ORGANIZATION, "--project", PROJECT)
        );
        assertEquals(0, exitCode);
    }

    @Test
    void listPlansOutputsJsonArrayWithExpectedFields() {
        var out = captureStdout(args("list-plans", "--json", "--organization", ORGANIZATION, "--project", PROJECT));
        assertEquals(0, out.exitCode());
        assertTrue(out.text().contains("\"planId\""));
        assertTrue(out.text().contains("\"createdAt\""));
    }

    @Test
    void listPlansReturnsAllThreePlans() {
        var out = captureStdout(args("list-plans", "--json", "--organization", ORGANIZATION, "--project", PROJECT));
        assertEquals(0, out.exitCode());
        assertEquals(3, countOccurrences(out.text(), "\"planId\""));
    }

    @Test
    void listPlansOrderedByCreatedAtDescending() {
        var out = captureStdout(args("list-plans", "--json", "--organization", ORGANIZATION, "--project", PROJECT));
        assertEquals(0, out.exitCode());
        // planIds[2] is newest, [1] middle, [0] oldest — desc order means [2] appears first
        int pos0 = out.text().indexOf(planIds.get(0));
        int pos1 = out.text().indexOf(planIds.get(1));
        int pos2 = out.text().indexOf(planIds.get(2));
        assertTrue(pos2 < pos1 && pos1 < pos0, "Newest plan should appear first in the output");
    }

    @Test
    void listPlansWithMaxLimitsResults() {
        var out = captureStdout(args("list-plans", "--json", "--organization", ORGANIZATION, "--project", PROJECT, "--max", "2"));
        assertEquals(0, out.exitCode());
        assertEquals(2, countOccurrences(out.text(), "\"planId\""));
    }

    @Test
    void listPlansWithOffsetSkipsNewestRecords() {
        // Desc order: [newest, middle, oldest]. Offset=1 skips newest -> [middle, oldest]
        var out = captureStdout(args("list-plans", "--json", "--organization", ORGANIZATION, "--project", PROJECT, "--offset", "1"));
        assertEquals(0, out.exitCode());
        assertEquals(2, countOccurrences(out.text(), "\"planId\""));
        assertFalse(out.text().contains(planIds.get(2)), "Newest plan should be skipped by offset=1");
        assertTrue(out.text().contains(planIds.get(1)));
        assertTrue(out.text().contains(planIds.get(0)));
    }

    @Test
    void listPlansWithOffsetAndMaxPaginatesCorrectly() {
        // Desc order: [newest, middle, oldest]. offset=1, max=1 -> [middle]
        var out = captureStdout(args("list-plans", "--json", "--organization", ORGANIZATION, "--project", PROJECT, "--offset", "1", "--max", "1"));
        assertEquals(0, out.exitCode());
        assertEquals(1, countOccurrences(out.text(), "\"planId\""));
        assertTrue(out.text().contains(planIds.get(1)), "Middle plan should be returned");
        assertFalse(out.text().contains(planIds.get(2)));
        assertFalse(out.text().contains(planIds.get(0)));
    }

    @Test
    void listPlansReturnsEmptyArrayForUnknownProject() {
        var out = captureStdout(args("list-plans", "--json", "--organization", "Unknown", "--project", "Unknown"));
        assertEquals(0, out.exitCode());
        assertEquals("[]", out.text().trim());
    }

    @Test
    void listPlansDoesNotIncludePlansFromOtherProject() {
        var out = captureStdout(args("list-plans", "--json", "--organization", ORGANIZATION, "--project", PROJECT));
        assertEquals(0, out.exitCode());
        assertEquals(3, countOccurrences(out.text(), "\"planId\""),
            "Plans from other project must not appear");
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
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) {
            count++;
            idx += token.length();
        }
        return count;
    }

    static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }
}
