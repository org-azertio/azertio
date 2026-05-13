package org.azertio.lsp.test;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.Test;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.azertio.lsp.AzertioLanguageServer;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class AzertioLanguageServerTest {

    private final AzertioLanguageServer server =
        new AzertioLanguageServer(null, new DefaultKeywordMapProvider(), null);

    @Test
    void initialize_returnsCapsWithCompletionAndCodeAction() throws Exception {
        var params = new InitializeParams();
        InitializeResult result = server.initialize(params).get();
        var caps = result.getCapabilities();
        assertThat(caps.getTextDocumentSync().getLeft()).isEqualTo(TextDocumentSyncKind.Full);
        assertThat(caps.getCompletionProvider()).isNotNull();
        assertThat(caps.getCodeActionProvider().getLeft()).isTrue();
    }

    @Test
    void shutdown_returnsNull() throws Exception {
        Object result = server.shutdown().get();
        assertThat(result).isNull();
    }

    @Test
    void getTextDocumentService_isNotNull() {
        assertThat(server.getTextDocumentService()).isNotNull();
    }

    @Test
    void getWorkspaceService_isNotNull() {
        assertThat(server.getWorkspaceService()).isNotNull();
    }

    @Test
    void connect_setsClientOnTextDocumentService() {
        LanguageClient client = new LanguageClient() {
            @Override public void telemetryEvent(Object o) {}
            @Override public void publishDiagnostics(PublishDiagnosticsParams p) {}
            @Override public void showMessage(MessageParams m) {}
            @Override public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams p) {
                return CompletableFuture.completedFuture(null);
            }
            @Override public void logMessage(MessageParams m) {}
        };
        // Should not throw
        server.connect(client);
    }
}