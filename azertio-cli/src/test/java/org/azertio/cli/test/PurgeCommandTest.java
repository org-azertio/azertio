package org.azertio.cli.test;

import org.junit.jupiter.api.Test;
import org.azertio.core.AzertioConfig;
import org.azertio.cli.MainCommand;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PurgeCommandTest {

	private static final String ENV_PATH = "target/.azertio-purge";

	@Test
	void showHelp() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"purge", "--help",
			"-D"+ AzertioConfig.ENV_PATH+"="+ENV_PATH,
			"-f","src/test/resources/azertio.yaml"
		);
		assertEquals(0, exitCode);
	}

	@Test
	void purgeTest() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"purge",
			"-D"+ AzertioConfig.ENV_PATH+"="+ENV_PATH,
			"-f","src/test/resources/azertio.yaml"
		);
		assertEquals(0, exitCode);
	}


}
