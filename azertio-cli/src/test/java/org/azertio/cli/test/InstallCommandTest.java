package org.azertio.cli.test;

import org.junit.jupiter.api.Test;
import org.azertio.core.AzertioConfig;
import org.azertio.cli.MainCommand;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InstallCommandTest {

	private static final String ENV_PATH = "target/.azertio-install";

	@Test
	void showHelp() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"install", "--help",
			"-f","src/test/resources/azertio.yaml",
			"-D"+AzertioConfig.ENV_PATH+"="+ENV_PATH
		);
		assertEquals(0, exitCode);
	}

	@Test
	void installTest() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"install",
			"-f","src/test/resources/azertio.yaml",
			"-D"+AzertioConfig.ENV_PATH+"="+ENV_PATH
		);
		assertEquals(0, exitCode);
	}

	@Test
	void installTestWithClean() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"install",
			"-D"+AzertioConfig.ENV_PATH+"="+ENV_PATH,
			"-f","src/test/resources/azertio.yaml",
			"--clean"
		);
		assertEquals(0, exitCode);
	}

}
