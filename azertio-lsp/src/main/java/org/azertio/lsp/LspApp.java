package org.azertio.lsp;

import org.eclipse.lsp4j.launch.LSPLauncher;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.backend.StepProviderBackend;

public class LspApp {

    public static void launch(AzertioRuntime runtime) {
        var backend = runtime != null ? new StepProviderBackend(runtime) : null;
        var config  = runtime != null ? runtime.configuration() : null;
        var keywordProvider = new DefaultKeywordMapProvider();
        var server = new AzertioLanguageServer(backend, keywordProvider, config);
        var launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);
        server.connect(launcher.getRemoteProxy());
        try {
            launcher.startListening().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new RuntimeException("LSP server stopped unexpectedly: " + e.getMessage(), e);
        }
    }
}
