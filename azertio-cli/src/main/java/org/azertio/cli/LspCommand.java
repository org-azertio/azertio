package org.azertio.cli;

import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioRuntime;
import org.azertio.lsp.LspApp;
import picocli.CommandLine;
import java.nio.file.Path;

@CommandLine.Command(
	name = "lsp",
	description = "Launch the LSP language server (communicates via stdio)"
)
public final class LspCommand extends AbstractCommand {

	@Override
	protected void execute() {
		LogConfig.redirectToFile(Path.of(System.getProperty("user.home"), ".azertio", "azertio.log"));
		AzertioRuntime runtime = null;
		try {
			AzertioContext context = getContext();
			runtime = new AzertioRuntime(context.configuration());
		} catch (Exception ignored) {
			// No azertio.yaml found — start in degraded mode (structural completions only)
		}
		LspApp.launch(runtime);
	}
}
