package org.azertio.cli;

import picocli.CommandLine;

@CommandLine.Command(
	name = "version",
	description = "Print version information"
)
public class VersionCommand implements Runnable {

	@Override
	public void run() {
		System.out.println("1.0.0-alpha1");
	}

}