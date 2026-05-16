package org.azertio.cli;

import picocli.CommandLine;
import java.util.List;
import java.util.Map;

@CommandLine.Command(
	name = "azertio",
	description = "Azertio CLI utility",
	subcommands = {
		BrowseCommand.class,
		DeleteExecutionCommand.class,
		DeletePlanCommand.class,
		ExecCommand.class,
		GetExecutionNodeCommand.class,
		InitCommand.class,
		InstallCommand.class,
		ListContributorsCommand.class,
		ListExecutionsCommand.class,
		ListPlansCommand.class,
		VersionCommand.class,
		PurgeCommand.class,
		PlanCommand.class,
		ReportCommand.class,
		ServeCommand.class,
		ShowConfigCommand.class,
		LspCommand.class
	}
)
public class MainCommand implements Runnable {

	/** Set to true by ExecCommand in --detach mode to prevent System.exit(). */
	public static volatile boolean detachModeActive = false;

	@CommandLine.Option(
		names = {"-f", "--file"},
		description = "Configuration file",
		scope = CommandLine.ScopeType.INHERIT,
		defaultValue = "azertio.yaml"
	)
	String configurationFile;

	@CommandLine.Option(
		names = {"-d", "--debug"},
		description = "Enable debug mode",
		scope = CommandLine.ScopeType.INHERIT,
		defaultValue = "false"
	)
	boolean debugMode;

	@CommandLine.Option(
		names = {"-s", "--suite"},
		description = "Test suite names",
		scope = CommandLine.ScopeType.INHERIT
	)
	List<String> suites;

	@CommandLine.Option(
		names = "-D",
		description = "Parameters in key=value format",
		scope = CommandLine.ScopeType.INHERIT
	)
	Map<String, String> params;



	@CommandLine.Option(
		names = {"-P", "--profile"},
		description = "Profile name to activate",
		scope = CommandLine.ScopeType.INHERIT
	)
	String profile;

	@CommandLine.Option(
		names = {"--help"},
		description = "Show command help",
		scope = CommandLine.ScopeType.INHERIT
	)
	boolean showHelp;

	@CommandLine.Spec
	CommandLine.Model.CommandSpec spec;


	public static void main(String[] args) {
		int exitCode = new CommandLine(new MainCommand()).execute(args);
		if (!detachModeActive) {
			System.exit(exitCode);
		}
	}

	@Override
	public void run() {
		spec.commandLine().usage(spec.commandLine().getOut());
	}

}
