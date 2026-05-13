package org.azertio.cli.test;

import org.junit.jupiter.api.Test;
import org.azertio.cli.MainCommand;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainCommandTest {

	@Test
	void noArgsShowsHelp() {
		int exitCode = new CommandLine(new MainCommand()).execute();
		assertEquals(0, exitCode);
	}

	@Test
	void helptShowsHelp() {
		int exitCode = new CommandLine(new MainCommand()).execute("--help");
		assertEquals(0, exitCode);
	}

}