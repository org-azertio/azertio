package org.azertio.cli.test;

import org.azertio.cli.MainCommand;
import org.azertio.core.AzertioConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportCommandTest {

    static Path yaml;
    static Path featuresDir;
    static Path envPath;

    static final String YAML_CONTENT = """
            project:
              organization: Test Org
              name: Report Test
              test-suites:
                - name: default
                  description: Default
                  tag-expression: ""
            plugins: []
            configuration:
              core:
                resourceFilter: '**/*.feature'
            profiles: {}
            """;

    @BeforeAll
    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void setUp(@TempDir Path tempDir) throws Exception {
        yaml = tempDir.resolve("azertio.yaml");
        Files.writeString(yaml, YAML_CONTENT);
        featuresDir = tempDir.resolve("features");
        Files.createDirectories(featuresDir);
        envPath = tempDir.resolve(".azertio");
        Files.createDirectories(envPath);
    }

    // ─── last-execution: no executions ───────────────────────────────────────

    @Test
    void report_lastExecution_noExecutions_failsWithMessage() {
        var out = capture(
            "report",
            "-f", yaml.toString(),
            "-D" + AzertioConfig.ENV_PATH + "=" + envPath,
            "-D" + AzertioConfig.PERSISTENCE_MODE + "=" + AzertioConfig.PERSISTENCE_MODE_TRANSIENT,
            "-D" + AzertioConfig.RESOURCE_PATH + "=" + featuresDir,
            "--last-execution"
        );
        assertThat(out.exitCode()).isEqualTo(1);
        assertThat(out.stderr()).contains("No executions found");
    }

    // ─── execution-id: unknown id ────────────────────────────────────────────

    @Test
    void report_executionId_notFound_failsWithMessage() {
        UUID randomId = UUID.randomUUID();
        var out = capture(
            "report",
            "-f", yaml.toString(),
            "-D" + AzertioConfig.ENV_PATH + "=" + envPath,
            "-D" + AzertioConfig.PERSISTENCE_MODE + "=" + AzertioConfig.PERSISTENCE_MODE_TRANSIENT,
            "-D" + AzertioConfig.RESOURCE_PATH + "=" + featuresDir,
            "--execution-id", randomId.toString()
        );
        assertThat(out.exitCode()).isEqualTo(1);
        assertThat(out.stderr()).contains("not found");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    record CapturedOutput(int exitCode, String stdout, String stderr) {}

    static CapturedOutput capture(String... args) {
        var stdoutBuf = new ByteArrayOutputStream();
        var stderrBuf = new ByteArrayOutputStream();
        var cmd = new CommandLine(new MainCommand());
        cmd.setOut(new PrintWriter(new OutputStreamWriter(stdoutBuf, StandardCharsets.UTF_8), true));
        cmd.setErr(new PrintWriter(new OutputStreamWriter(stderrBuf, StandardCharsets.UTF_8), true));
        int code = cmd.execute(args);
        return new CapturedOutput(
            code,
            stdoutBuf.toString(StandardCharsets.UTF_8),
            stderrBuf.toString(StandardCharsets.UTF_8)
        );
    }
}