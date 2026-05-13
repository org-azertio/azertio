package org.azertio.cli.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.cli.MainCommand;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioFile;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.execution.ExecutionResult;
import org.azertio.core.persistence.TestExecutionRepository;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.NodeType;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestPlanNode;
import org.azertio.core.testplan.TestProject;
import picocli.CommandLine;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GetExecutionNodeCommandTest {

    static final String ENV_PATH = "target/.azertio-getexecnode";
    static final String[] BASE_ARGS = {
        "-f", "src/test/resources/azertio.yaml",
        "-D" + AzertioConfig.ENV_PATH + "=" + ENV_PATH,
        "-D" + AzertioConfig.PERSISTENCE_MODE + "=" + AzertioConfig.PERSISTENCE_MODE_FILE
    };

    static String executionId;
    static String planNodeId;

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
            var context = file.createContext(
                Config.ofMap(Map.of(
                    AzertioConfig.ENV_PATH, ENV_PATH,
                    AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_FILE
                )),
                List.of()
            );
            AzertioRuntime runtime = new AzertioRuntime(context.configuration());
            TestPlanRepository planRepo = runtime.getRepository(TestPlanRepository.class);
            TestExecutionRepository execRepo = runtime.getRepository(TestExecutionRepository.class);

            UUID projectId = planRepo.persistProject(new TestProject("P", null, "Org", List.of()));
            UUID root = planRepo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
            planNodeId = root.toString();
            TestPlan plan = planRepo.persistPlan(new TestPlan(null, projectId, Instant.now(), "h", "c", root, 0, null));

            var ex = execRepo.newExecution(plan.planID(), Instant.now(), null);
            executionId = ex.executionID().toString();
            UUID execNodeId = execRepo.newExecutionNode(ex.executionID(), root);
            execRepo.updateExecutionNodeFinish(execNodeId, ExecutionResult.PASSED, Instant.now());
        }
    }

    @Test
    void showHelp() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("get-execution-node", "--help",
                "--execution-id", UUID.randomUUID().toString(),
                "--plan-node-id", UUID.randomUUID().toString())
        );
        assertEquals(0, exitCode);
    }

    @Test
    void getExecutionNodeText() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("get-execution-node",
                "--execution-id", executionId,
                "--plan-node-id", planNodeId)
        );
        assertEquals(0, exitCode);
    }

    @Test
    void getExecutionNodeJson() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("get-execution-node",
                "--execution-id", executionId,
                "--plan-node-id", planNodeId,
                "--json")
        );
        assertEquals(0, exitCode);
    }

    @Test
    void getExecutionNodeNotFoundReturnsError() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("get-execution-node",
                "--execution-id", UUID.randomUUID().toString(),
                "--plan-node-id", UUID.randomUUID().toString())
        );
        assertNotEquals(0, exitCode);
    }

    static String[] args(String... extra) {
        List<String> all = new ArrayList<>(Arrays.asList(extra));
        all.addAll(Arrays.asList(BASE_ARGS));
        return all.toArray(String[]::new);
    }

    static void deleteDirectory(Path dir) throws IOException {
        if (!dir.toFile().exists()) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }
}