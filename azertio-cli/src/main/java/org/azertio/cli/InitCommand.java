package org.azertio.cli;

import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.persistence.TestPlanRepository;
import picocli.CommandLine;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
	name = "init",
	description = "Create a skeleton azertio.yaml and initialize the local environment"
)
public final class InitCommand extends AbstractCommand {

	@CommandLine.Option(names = {"-o", "--organization"}, description = "Organization name (non-interactive)")
	String organization;

	@CommandLine.Option(names = {"-n", "--name"}, description = "Project name (non-interactive)")
	String projectName;

	@Override
	protected void execute() {
		Path target = Path.of(parent.configurationFile);
		boolean yamlExists = Files.exists(target);

		if (!yamlExists) {
			if (organization == null || projectName == null) {
				Console console = System.console();
				if (organization == null) organization = prompt(console, "Organization: ");
				if (projectName  == null) projectName  = prompt(console, "Project name: ");
			}
			try {
				Files.writeString(target, buildYaml(organization, projectName));
			} catch (IOException e) {
				throw new RuntimeException("Could not write " + target + ": " + e.getMessage(), e);
			}
			out().println("Created " + target.toAbsolutePath());
		}

		AzertioContext context = getContext();
		Path envPath = context.configuration()
			.get(AzertioConfig.ENV_PATH, Path::of)
			.orElse(AzertioConfig.ENV_DEFAULT_PATH);

		if (yamlExists && Files.exists(envPath)) {
			out().println("Already initialized at " + envPath.toAbsolutePath());
			return;
		}

		AzertioRuntime.repositoryOnly(context.configuration()).getRepository(TestPlanRepository.class);
		out().println("Initialized environment at " + envPath.toAbsolutePath());
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