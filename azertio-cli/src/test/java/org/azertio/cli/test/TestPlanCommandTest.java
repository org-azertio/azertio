package org.azertio.cli.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.azertio.core.AzertioConfig;
import org.azertio.cli.MainCommand;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestPlanCommandTest {

	private static final String ENV_PATH = "target/.azertio-testplan";

	@BeforeAll
	static void installPlugins() {
		new CommandLine(new MainCommand()).execute(
			"install",
			"-f", "src/test/resources/azertio.yaml",
			"-D" + AzertioConfig.ENV_PATH + "=" + ENV_PATH
		);
	}

	@Test
	void showHelp() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"plan", "--help",
			"-f","src/test/resources/azertio.yaml",
			"-D"+AzertioConfig.ENV_PATH+"="+ENV_PATH
		);
		assertEquals(0, exitCode);
	}

	@Test
	void assembleTestPlan() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"plan",
			"-f","src/test/resources/azertio.yaml",
			"-D"+AzertioConfig.ENV_PATH+"="+ENV_PATH,
			"--suite", "suiteA",
			"-D"+AzertioConfig.PERSISTENCE_MODE+"="+AzertioConfig.PERSISTENCE_MODE_TRANSIENT,
			"-D"+AzertioConfig.RESOURCE_PATH+"=src/test/resources/test-features"
		);
		assertEquals(0, exitCode);
	}


	@Test
	void assembleTestPlanWithDetails() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"plan", "--detail",
			"-f","src/test/resources/azertio.yaml",
			"-D"+AzertioConfig.ENV_PATH+"="+ENV_PATH,
			"--suite", "suiteA",
			"-D"+AzertioConfig.PERSISTENCE_MODE+"="+AzertioConfig.PERSISTENCE_MODE_TRANSIENT,
			"-D"+AzertioConfig.RESOURCE_PATH+"=src/test/resources/test-features"
		);
		assertEquals(0, exitCode);
	}


}
