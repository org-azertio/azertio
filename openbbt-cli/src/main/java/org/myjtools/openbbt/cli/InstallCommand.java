package org.myjtools.openbbt.cli;

import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTPluginManager;
import org.myjtools.openbbt.core.util.Log;
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

		OpenBBTContext context = getContext();

		if (clean) {
			Path envPath = context.configuration().get(OpenBBTConfig.ENV_PATH, Path::of).orElse(OpenBBTConfig.ENV_DEFAULT_PATH);
			Path pluginsPath = envPath.resolve(OpenBBTConfig.PLUGINS_PATH);
			Util.deleteDirectory(pluginsPath);
			log.info("Existing plugins cleaned.");
		}


		if (context.plugins().isEmpty()) {
			log.info("Nothing to install");
			return;
		}

		OpenBBTPluginManager pluginManager = new OpenBBTPluginManager(context.configuration());
		for (String plugin : context.plugins()) {
			try {
				boolean result = pluginManager.installPlugin(plugin);
				if (!result) {
					throw new OpenBBTException("Failed to install plugin {}",plugin);
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
