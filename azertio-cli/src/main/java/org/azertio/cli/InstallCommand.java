package org.azertio.cli;

import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioException;
import org.azertio.core.AzertioPluginManager;
import org.azertio.core.util.Log;
import picocli.CommandLine;
import java.nio.file.Path;

@CommandLine.Command(
	name = "install",
	description = "Install plugins required by the project"
)
public final class InstallCommand extends AbstractCommand {

	private static final Log log = Log.of();

	@CommandLine.Option(
		names = {"-c", "--clean"},
		description = "Clean existing plugins before installation",
		defaultValue = "false"
	)
	boolean clean;


	@Override
	protected void execute() {

		AzertioContext context = getContext();

		if (clean) {
			Path envPath = context.configuration().get(AzertioConfig.ENV_PATH, Path::of).orElse(AzertioConfig.ENV_DEFAULT_PATH);
			Path pluginsPath = envPath.resolve(AzertioConfig.PLUGINS_PATH);
			Util.deleteDirectory(pluginsPath);
			log.info("Existing plugins cleaned.");
		}


		if (context.plugins().isEmpty()) {
			log.info("Nothing to install");
			return;
		}

		AzertioPluginManager pluginManager = new AzertioPluginManager(context.configuration());
		for (String plugin : context.plugins()) {
			try {
				boolean result = pluginManager.installPlugin(plugin);
				if (!result) {
					throw new AzertioException("Failed to install plugin {}",plugin);
				}
			} catch (Exception e) {
				log.error(e,"Failed to install plugin {}", plugin);
			}
		}
		if (parent.debugMode) {
			log.info(pluginManager.moduleLayerTreeDescription());
		}
	}


}
