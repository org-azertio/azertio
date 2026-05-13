package org.azertio.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.imconfig.Config;
import org.azertio.core.backend.StepProviderBackend;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AzertioLanguageServer implements LanguageServer, LanguageClientAware {

    private final AzertioTextDocumentService textDocumentService;
    private final AzertioWorkspaceService workspaceService;

    public AzertioLanguageServer(StepProviderBackend backend, DefaultKeywordMapProvider keywordProvider, Config config) {
        this.textDocumentService = new AzertioTextDocumentService(backend, keywordProvider, config);
        this.workspaceService = new AzertioWorkspaceService();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        var capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        var completionOptions = new CompletionOptions();
        completionOptions.setTriggerCharacters(List.of(" "));
        capabilities.setCompletionProvider(completionOptions);
        capabilities.setCodeActionProvider(true);
        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        textDocumentService.setClient(client);
    }
}
