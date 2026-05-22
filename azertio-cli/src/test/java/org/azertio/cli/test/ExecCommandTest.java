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

import static org.assertj.core.api.Assertions.assertThat;

class ExecCommandTest {

    static Path yaml;
    static Path featuresDir;
    static Path envPath;

    static final String YAML_CONTENT = """
            project:
              organization: Test Org
              name: Exec Test
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

    // ─── help ────────────────────────────────────────────────────────────────

    @Test
    void showHelp() {
        int exitCode = new CommandLine(new MainCommand()).execute("exec", "--help",
            "-f", yaml.toString());
        assertThat(exitCode).isZero();
    }

    // ─── attached execution ───────────────────────────────────────────────────

    @Test
    void exec_withEmptyFeaturesDir_runsAndExitsZero() {
        var out = capture(
            "exec",
            "-f", yaml.toString(),
            "-D" + AzertioConfig.ENV_PATH + "=" + envPath,
            "-D" + AzertioConfig.PERSISTENCE_MODE + "=" + AzertioConfig.PERSISTENCE_MODE_FILE,
            "-D" + AzertioConfig.PERSISTENCE_FILE + "=db/azertio.db",
            "-D" + AzertioConfig.RESOURCE_PATH + "=" + featuresDir,
            "--exit-zero"
        );
        assertThat(out.stderr()).as("stderr").isEmpty();
        assertThat(out.exitCode()).isZero();
    }

    @Test
    void exec_json_outputsJsonResult() {
        var out = capture(
            "exec",
            "-f", yaml.toString(),
            "-D" + AzertioConfig.ENV_PATH + "=" + envPath,
            "-D" + AzertioConfig.PERSISTENCE_MODE + "=" + AzertioConfig.PERSISTENCE_MODE_FILE,
            "-D" + AzertioConfig.PERSISTENCE_FILE + "=db/azertio.db",
            "-D" + AzertioConfig.RESOURCE_PATH + "=" + featuresDir,
            "--json",
            "--exit-zero"
        );
        assertThat(out.stderr()).as("stderr").isEmpty();
        assertThat(out.exitCode()).isZero();
        assertThat(out.stdout()).contains("executionId");
    }

    // ─── detached execution ───────────────────────────────────────────────────

    @Test
    void exec_detach_printsExecutionId() {
        var out = capture(
            "exec",
            "-f", yaml.toString(),
            "-D" + AzertioConfig.ENV_PATH + "=" + envPath,
            "-D" + AzertioConfig.PERSISTENCE_MODE + "=" + AzertioConfig.PERSISTENCE_MODE_FILE,
            "-D" + AzertioConfig.PERSISTENCE_FILE + "=db/azertio.db",
            "-D" + AzertioConfig.RESOURCE_PATH + "=" + featuresDir,
            "--detach"
        );
        assertThat(out.stderr()).as("stderr").isEmpty();
        assertThat(out.exitCode()).isZero();
        assertThat(out.stdout().trim()).isNotEmpty();
    }

    @Test
    void exec_detach_json_outputsJsonExecutionId() {
        var out = capture(
            "exec",
            "-f", yaml.toString(),
            "-D" + AzertioConfig.ENV_PATH + "=" + envPath,
            "-D" + AzertioConfig.PERSISTENCE_MODE + "=" + AzertioConfig.PERSISTENCE_MODE_FILE,
            "-D" + AzertioConfig.PERSISTENCE_FILE + "=db/azertio.db",
            "-D" + AzertioConfig.RESOURCE_PATH + "=" + featuresDir,
            "--detach",
            "--json"
        );
        assertThat(out.stderr()).as("stderr").isEmpty();
        assertThat(out.exitCode()).isZero();
        assertThat(out.stdout()).contains("executionId");
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