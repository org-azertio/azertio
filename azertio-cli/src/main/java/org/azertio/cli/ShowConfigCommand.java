package org.azertio.cli;

import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.util.Log;
import picocli.CommandLine;

@CommandLine.Command(
	name = "show-config",
	description = "Show the available configuration options and their current values"
)
public final class ShowConfigCommand extends AbstractCommand {

	private static final Log log = Log.of();


	@Override
	protected void execute() {
		log.debug("Showing configuration options...");
		AzertioContext context = getContext();
		AzertioRuntime cm = new AzertioRuntime(context.configuration());
		out().println(ConfigFormatter.toMaskedString(cm.configuration()));
		out().println("Available configuration options:");
		out().println();
		out().println(cm.configuration().getDefinitionsToString());

	}


}
