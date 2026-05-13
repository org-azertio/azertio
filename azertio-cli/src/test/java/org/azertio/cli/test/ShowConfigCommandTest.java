package org.azertio.cli.test;

import org.junit.jupiter.api.Test;
import org.azertio.core.AzertioConfig;
import org.azertio.cli.MainCommand;
import picocli.CommandLine;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowConfigCommandTest {

	private static final String ENV_PATH = "target/.azertio-show-config";

	@Test
	void showHelp() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"show-config", "--help",
			"-f","src/test/resources/azertio.yaml",
			"-D"+AzertioConfig.ENV_PATH+"="+ENV_PATH
		);
		assertEquals(0, exitCode);
	}

	@Test
	void showConfig() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"show-config",
			"--debug",
			"-f","src/test/resources/azertio.yaml",
			"-D"+AzertioConfig.ENV_PATH+"="+ENV_PATH,
			"-Dparam1=value1"
		);
		assertEquals(0, exitCode);
	}

	@Test
	void showHelpWritesUsageToConfiguredStdout() {
		var output = captureOutput(
			"show-config", "--help",
			"-f", "src/test/resources/azertio.yaml",
			"-D" + AzertioConfig.ENV_PATH + "=" + ENV_PATH
		);

		assertThat(output.exitCode()).isZero();
		assertThat(output.stdout()).contains("Show the available configuration options");
		assertThat(output.stderr()).isEmpty();
	}

	@Test
	void showConfigWritesErrorsToConfiguredStderr() {
		var output = captureOutput(
			"show-config",
			"-f", "src/test/resources/missing-azertio.yaml",
			"-D" + AzertioConfig.ENV_PATH + "=" + ENV_PATH
		);

		assertThat(output.exitCode()).isEqualTo(1);
		assertThat(output.stdout()).isEmpty();
		assertThat(output.stderr()).contains("Failed to read configuration file");
	}

	private static CapturedOutput captureOutput(String... args) {
		var stdout = new ByteArrayOutputStream();
		var stderr = new ByteArrayOutputStream();
		CommandLine commandLine = new CommandLine(new MainCommand());
		commandLine.setOut(new PrintWriter(new OutputStreamWriter(stdout, StandardCharsets.UTF_8), true));
		commandLine.setErr(new PrintWriter(new OutputStreamWriter(stderr, StandardCharsets.UTF_8), true));
		int exitCode = commandLine.execute(args);
		return new CapturedOutput(
			exitCode,
			stdout.toString(StandardCharsets.UTF_8),
			stderr.toString(StandardCharsets.UTF_8)
		);
	}

	private record CapturedOutput(int exitCode, String stdout, String stderr) {}

}
