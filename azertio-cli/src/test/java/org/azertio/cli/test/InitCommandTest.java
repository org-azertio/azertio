package org.azertio.cli.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.azertio.core.AzertioConfig;
import org.azertio.cli.MainCommand;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InitCommandTest {

    @Test
    void showHelp() {
        int exitCode = new CommandLine(new MainCommand()).execute("init", "--help");
        assertEquals(0, exitCode);
    }

    @Test
    void init_createsYamlAndDatabase(@TempDir Path tempDir) throws Exception {
        Path yaml = tempDir.resolve("azertio.yaml");
        Path envPath = tempDir.resolve(".azertio");

        int exitCode = run(tempDir, yaml, envPath);

        assertEquals(0, exitCode);
        assertTrue(Files.exists(yaml), "azertio.yaml must be created");
        String content = Files.readString(yaml);
        assertTrue(content.contains("organization: My Org"));
        assertTrue(content.contains("name: My Project"));
        assertTrue(Files.exists(envPath), ".azertio directory must be created");
        assertTrue(Files.exists(envPath.resolve("db/azertio.db.mv.db")), "H2 database file must exist");
    }

    @Test
    void init_isIdempotent_whenAlreadyInitialized(@TempDir Path tempDir) throws Exception {
        Path yaml = tempDir.resolve("azertio.yaml");
        Path envPath = tempDir.resolve(".azertio");

        // First run
        assertEquals(0, run(tempDir, yaml, envPath));

        // Second run must succeed and report already initialized
        var out = new ByteArrayOutputStream();
        CommandLine cmd = new CommandLine(new MainCommand());
        cmd.setOut(new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true));
        cmd.setErr(new PrintWriter(new OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8), true));
        int exitCode = cmd.execute(
            "init",
            "-f", yaml.toString(),
            "-o", "My Org",
            "-n", "My Project",
            "-D" + AzertioConfig.ENV_PATH + "=" + envPath
        );

        assertEquals(0, exitCode);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Already initialized"));
    }

    @Test
    void init_initializesDatabase_whenYamlExistsButEnvMissing(@TempDir Path tempDir) throws Exception {
        Path yaml = tempDir.resolve("azertio.yaml");
        Path envPath = tempDir.resolve(".azertio");

        // Pre-create the yaml without the env
        Files.writeString(yaml, """
            project:
              organization: My Org
              name: My Project
              test-suites: []
            plugins:
              - gherkin
            configuration:
              core:
                resourceFilter: '**/*.feature'
            profiles: {}
            """);

        int exitCode = run(tempDir, yaml, envPath);

        assertEquals(0, exitCode);
        assertTrue(Files.exists(envPath.resolve("db/azertio.db.mv.db")), "H2 database file must exist");
    }

    // --- helpers ---

    private static int run(Path tempDir, Path yaml, Path envPath) {
        return new CommandLine(new MainCommand()).execute(
            "init",
            "-f", yaml.toString(),
            "-o", "My Org",
            "-n", "My Project",
            "-D" + AzertioConfig.ENV_PATH + "=" + envPath
        );
    }
}