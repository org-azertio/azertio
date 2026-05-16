package org.azertio.cli;

import com.google.common.io.Files;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioException;
import org.azertio.core.AzertioFile;
import org.azertio.core.execution.Profile;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

public abstract sealed class AbstractCommand implements Callable<Integer> permits BrowseCommand, DeleteExecutionCommand, DeletePlanCommand, ExecCommand, GetExecutionNodeCommand, InitCommand, InstallCommand, ListContributorsCommand, ListExecutionsCommand, ListPlansCommand, LspCommand, PlanCommand, PurgeCommand, ServeCommand, ShowConfigCommand {

	@CommandLine.ParentCommand
	MainCommand parent;

	@CommandLine.Spec
	CommandLine.Model.CommandSpec spec;

	protected int exitCode = 0;

	protected abstract void execute();

	protected PrintWriter out() {
		return spec.commandLine().getOut();
	}

	protected PrintWriter err() {
		return spec.commandLine().getErr();
	}


	protected AzertioContext getContext() {
		Map<String, String> cliParams = parent.params == null ? Map.of() : parent.params;
		Map<String, String> envParams = System.getenv();
		Map<String, String> envFileParams = readEnvFileParams();
		return readConfigurationFile().createContext(
			Config.ofMap(combineParams(cliParams, envFileParams, envParams)),
			parent.suites == null ? List.of() : parent.suites
		);
	}


	private Map<String,String> combineParams(Map<String, String>... params) {
		var combined = new LinkedHashMap<String, String>();
		for (Map<String, String> paramSource : params) {
			paramSource.forEach(combined::putIfAbsent);
		}
		return combined;
	}

	protected List<String> getSelectedSuites() {
		return parent.suites == null ? List.of() : parent.suites;
	}

	protected AzertioFile readConfigurationFile() {
		try (var reader = Files.newReader(new File(parent.configurationFile), java.nio.charset.StandardCharsets.UTF_8)) {
		    return AzertioFile.read(reader);
		} catch (Exception e) {
			throw new AzertioException(
				e,
				"Failed to read configuration file: {}",
				e.getMessage()
			);
		}
	}


	protected Map<String, String> readEnvFileParams() {
		var file = new File(parent.configurationFile).getAbsoluteFile().getParentFile().toPath().resolve(".env").toFile();
		if (!file.exists()) {
			return Map.of();
		}
		try (var reader = Files.newReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
			Properties properties = new Properties();
			properties.load(reader);
			return properties.entrySet().stream()
				.collect(java.util.stream.Collectors.toMap(
					e -> e.getKey().toString(),
					e -> e.getValue().toString()
				));
		} catch (Exception e) {
			throw new AzertioException(e, "Failed to read env file: {}", e.getMessage());
		}
	}


	protected Profile profile(String profileName) {
		if (profileName == null || profileName.isBlank()) {
			return Profile.NONE;
		}
		var profiles = readConfigurationFile().profiles();
		var properties = profiles.get(profileName);
		if (properties == null) {
			throw new AzertioException("Profile '{}' not found in configuration", profileName);
		}
		return new Profile(profileName, properties);

	}

	@Override
	public Integer call() throws Exception {
		if (parent.debugMode) {
			try {
				var cl = Thread.currentThread().getContextClassLoader();
				var levelClass = Class.forName("ch.qos.logback.classic.Level", true, cl);
				var debugLevel = levelClass.getField("DEBUG").get(null);
				// Set debug for org.myjtools (includes jexten and azertio)
				var root = LoggerFactory.getLogger("org.myjtools");
				root.getClass().getMethod("setLevel", levelClass).invoke(root, debugLevel);
			} catch (ReflectiveOperationException ignored) {}
		}
		try {
			if (parent.showHelp) {
				spec.commandLine().usage(out());
				return 0;
			}
			execute();
			out().flush();
			return exitCode;
		} catch (Exception e) {
			err().println(e.getMessage());
			return 1;
		}
	}
}
