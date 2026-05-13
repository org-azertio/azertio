package org.azertio.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.azertio.cli.MainCommand;
import org.azertio.core.AzertioConfig;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnvFileCommandIntegrationTest {

	private static final String FIXTURE_DIR = "src/test/resources/env-cli";
	private static final String CONFIG_FILE = FIXTURE_DIR + "/azertio.yaml";
	private static final String FORMAT_ENV_FIXTURE_DIR = "src/test/resources/env-format-cli";
	private static final String FORMAT_ENV_CONFIG_FILE = FORMAT_ENV_FIXTURE_DIR + "/azertio.yaml";
	private static final String NO_ENV_FIXTURE_DIR = "src/test/resources/no-env-cli";
	private static final String NO_ENV_CONFIG_FILE = NO_ENV_FIXTURE_DIR + "/azertio.yaml";
	private static final String ENV_LOADED_TAG = "env-loaded-definition-tag";
	private static final String ENV_SECRET_VALUE = "env-secret-value";
	private static final String PROCESS_ENV_KEY = "USER";
	private static final String ENV_FILE_PRECEDENCE_VALUE = "from-env-file";
	private static final String CLI_PRECEDENCE_VALUE = "from-cli";

	@Test
	void showConfig_readsSiblingEnvFileWithoutPrintingSecretsEvenInDebugMode() {
		var out = captureStdout(args("show-config", "--debug"));

		assertThat(out.exitCode()).isZero();
		assertThat(out.text()).contains(ENV_LOADED_TAG);
		assertThat(out.text()).contains("core.artifacts.repository.password : <masked>");
		assertThat(out.text()).doesNotContain(ENV_SECRET_VALUE);
	}

	@Test
	void showConfig_envFileOverridesProcessEnvironment() {
		assertThat(System.getenv(PROCESS_ENV_KEY)).isNotBlank().isNotEqualTo(ENV_FILE_PRECEDENCE_VALUE);

		var out = captureStdout(args("show-config"));

		assertThat(out.exitCode()).isZero();
		assertThat(out.text()).contains(PROCESS_ENV_KEY + " : " + ENV_FILE_PRECEDENCE_VALUE);
	}

	@Test
	void showConfig_cliOverridesEnvFile() {
		var out = captureStdout(args("show-config", "-D" + PROCESS_ENV_KEY + "=" + CLI_PRECEDENCE_VALUE));

		assertThat(out.exitCode()).isZero();
		assertThat(out.text()).contains(PROCESS_ENV_KEY + " : " + CLI_PRECEDENCE_VALUE);
	}

	@Test
	void showConfig_withoutSiblingEnv_usesDefaultAndDoesNotLeakOtherFixtureValues() {
		var out = captureOutput(argsForConfig(NO_ENV_FIXTURE_DIR, NO_ENV_CONFIG_FILE, "show-config"));

		assertThat(out.exitCode()).isZero();
		assertThat(out.stdout()).contains("core.definitionTag : definition");
		assertThat(out.stdout()).doesNotContain(ENV_LOADED_TAG);
		assertThat(out.stdout()).doesNotContain(PROCESS_ENV_KEY + " : " + ENV_FILE_PRECEDENCE_VALUE);
	}

	@Test
	void showConfig_withUnreadableEnvFile_returnsClearError(@TempDir Path tempDir) throws Exception {
		Path configFile = tempDir.resolve("azertio.yaml");
		Files.writeString(configFile, Files.readString(Path.of(CONFIG_FILE)), StandardCharsets.UTF_8);
		Files.createDirectory(tempDir.resolve(".env"));

		var out = captureOutput(argsForConfig(tempDir.toString(), configFile.toString(), "show-config"));

		assertThat(out.exitCode()).isEqualTo(1);
		assertThat(out.stderr()).contains("Failed to read env file");
	}

	@Test
	void showConfig_parsesCommentsBlankLinesAndSpacesAroundEquals() {
		var out = captureOutput(argsForConfig(FORMAT_ENV_FIXTURE_DIR, FORMAT_ENV_CONFIG_FILE, "show-config"));

		assertThat(out.exitCode()).isZero();
		assertThat(out.stdout()).contains("core.definitionTag : env-format-tag");
		assertThat(out.stdout()).contains(PROCESS_ENV_KEY + " : from-format-env");
	}

	private static String[] args(String... extra) {
		return argsForConfig(FIXTURE_DIR, CONFIG_FILE, extra);
	}

	private static String[] argsForConfig(String fixtureDir, String configFile, String... extra) {
		List<String> all = new ArrayList<>(Arrays.asList(extra));
		all.addAll(Arrays.asList(
			"-f", configFile,
			"-D" + AzertioConfig.RESOURCE_PATH + "=" + fixtureDir,
			"-D" + AzertioConfig.RESOURCE_FILTER + "=azertio.yaml",
			"-D" + AzertioConfig.PERSISTENCE_MODE + "=" + AzertioConfig.PERSISTENCE_MODE_TRANSIENT
		));
		return all.toArray(String[]::new);
	}

	private record CapturedOutput(int exitCode, String stdout, String stderr) {
		String text() {
			return stdout;
		}
	}

	private static CapturedOutput captureStdout(String... args) {
		CapturedOutput output = captureOutput(args);
		return new CapturedOutput(output.exitCode(), output.stdout(), output.stderr());
	}

	private static CapturedOutput captureOutput(String... args) {
		var stdoutBuffer = new ByteArrayOutputStream();
		var stderrBuffer = new ByteArrayOutputStream();
		var commandLine = new CommandLine(new MainCommand());
		commandLine.setOut(new PrintWriter(new OutputStreamWriter(stdoutBuffer, StandardCharsets.UTF_8), true));
		commandLine.setErr(new PrintWriter(new OutputStreamWriter(stderrBuffer, StandardCharsets.UTF_8), true));
		int code = commandLine.execute(args);
		return new CapturedOutput(code, stdoutBuffer.toString(StandardCharsets.UTF_8), stderrBuffer.toString(StandardCharsets.UTF_8));
	}
}
