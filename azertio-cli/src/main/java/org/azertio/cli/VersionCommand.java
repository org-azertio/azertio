package org.azertio.cli;

import picocli.CommandLine;

@CommandLine.Command(
	name = "version",
	description = "Print version information"
)
public class VersionCommand extends AbstractCommand {

	public static final String VERSION = "1.2.4";

	@Override
	public void execute() {
		out().println(VERSION);
	}

}