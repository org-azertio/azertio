package org.azertio.cli;

import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioContext;
import org.azertio.core.util.Log;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
	name = "purge",
	description = "Delete all local Azertio data"
)
public final class PurgeCommand extends AbstractCommand {

	private static final Log log = Log.of();


	@Override
	protected void execute() {
		log.info("Purging Azertio data...");
		AzertioContext context = getContext();
		Path envPath = context.configuration().get(AzertioConfig.ENV_PATH, Path::of).orElse(AzertioConfig.ENV_DEFAULT_PATH);
		if (envPath.toFile().exists()) {
			try (var stream = Files.list(envPath)) {
				stream.forEach(Util::deleteDirectory);
			} catch (IOException e) {
				log.error(e, "Failed to purge Azertio data: {}", e.getMessage());
			}
		}
	}




}
