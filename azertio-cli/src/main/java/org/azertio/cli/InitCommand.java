package org.azertio.cli;

import picocli.CommandLine;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
	name = "init",
	description = "Create a skeleton azertio.yaml in the current directory"
)
public final class InitCommand extends AbstractCommand {

	@CommandLine.Option(names = {"-o", "--organization"}, description = "Organization name (non-interactive)")
	String organization;

	@CommandLine.Option(names = {"-n", "--name"}, description = "Project name (non-interactive)")
	String projectName;

	@Override
	protected void execute() {
		Path target = Path.of(parent.configurationFile);

		if (Files.exists(target)) {
			throw new RuntimeException(target + " already exists. Remove it first if you want to reinitialise.");
		}

		if (organization == null || projectName == null) {
			Console console = System.console();
			if (organization == null) organization = prompt(console, "Organization: ");
			if (projectName  == null) projectName  = prompt(console, "Project name: ");
		}

		String yaml = buildYaml(organization, projectName);

		try {
			Files.writeString(target, yaml);
		} catch (IOException e) {
			throw new RuntimeException("Could not write " + target + ": " + e.getMessage(), e);
		}

		System.out.println("Created " + target.toAbsolutePath());
	}

	private String prompt(Console console, String message) {
		if (console == null) {
			throw new RuntimeException("No interactive console available. Cannot run 'init' non-interactively.");
		}
		String value = console.readLine(message);
		if (value == null || value.isBlank()) {
			throw new RuntimeException("Value required for: " + message.strip());
		}
		return value.strip();
	}

	private String buildYaml(String organization, String projectName) {
		return """
			project:
			  organization: %s
			  name: %s
			  test-suites:
			    - name: default
			      description: Default test suite
			      tag-expression: ""

			plugins:
			  - gherkin

			configuration:
			  core:
			    resourceFilter: '**/*.feature'

			profiles: {}
			""".formatted(organization, projectName);
	}
}
