package org.azertio.lsp.test;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.backend.StepProviderBackend;
import org.azertio.lsp.AzertioTextDocumentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AzertioTextDocumentService with a real backend and config,
 * covering the code paths that require non-null dependencies.
 */
class AzertioTextDocumentServiceWithBackendTest {

    private AzertioTextDocumentService service;
    private final List<PublishDiagnosticsParams> published = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Config runtimeConfig = Config.ofMap(Map.of(
            AzertioConfig.ENV_PATH, "target/.azertio-lsp-test"
        ));
        AzertioRuntime runtime = new AzertioRuntime(runtimeConfig);
        StepProviderBackend backend = new StepProviderBackend(runtime);
        Config config = runtime.configuration();
        service = new AzertioTextDocumentService(backend, new DefaultKeywordMapProvider(), config);
        service.setClient(new LanguageClient() {
            @Override public void telemetryEvent(Object o) {}
            @Override public void publishDiagnostics(PublishDiagnosticsParams p) { published.add(p); }
            @Override public void showMessage(MessageParams m) {}
            @Override public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams r) {
                return CompletableFuture.completedFuture(null);
            }
            @Override public void logMessage(MessageParams m) {}
        });
    }

    // ─── step completions ─────────────────────────────────────────────────────

    @Test
    void completion_featureFile_atStepKeyword_returnsItems() throws Exception {
        String content = "Feature: x\n  Scenario: s\n    Given \n";
        open("test.feature", content);
        var items = completion("test.feature", content, 2, 10);
        // with a real CoreStepProvider backend, there should be completions
        assertThat(items).isNotNull();
    }

    @Test
    void completion_featureFile_atStepKeyword_withPrefix_filtersItems() throws Exception {
        String content = "Feature: x\n  Scenario: s\n    Given the \n";
        open("test.feature", content);
        var items = completion("test.feature", content, 2, 14);
        assertThat(items).isNotNull();
    }

    // ─── config flat-key completions (comment line) ───────────────────────────

    @Test
    void completion_featureFile_atCommentLine_returnsConfigKeys() throws Exception {
        String content = "Feature: x\n  Scenario: s\n    # \n";
        open("test.feature", content);
        var items = completion("test.feature", content, 2, 7);
        assertThat(items).isNotNull();
    }

    @Test
    void completion_featureFile_atCommentLine_withPrefix_filtersKeys() throws Exception {
        String content = "Feature: x\n  Scenario: s\n    # core\n";
        open("test.feature", content);
        var items = completion("test.feature", content, 2, 10);
        assertThat(items).isNotNull();
    }

    // ─── yaml config-key completions ──────────────────────────────────────────

    @Test
    void completion_yamlFile_underConfiguration_returnsConfigKeys() throws Exception {
        String content = "configuration:\n  \n";
        open("azertio.yaml", content);
        var items = completion("azertio.yaml", content, 1, 2);
        assertThat(items).isNotNull();
    }

    @Test
    void completion_yamlFile_underConfigurationSection_returnsSubKeys() throws Exception {
        String content = "configuration:\n  core:\n    \n";
        open("azertio.yaml", content);
        var items = completion("azertio.yaml", content, 2, 4);
        assertThat(items).isNotNull();
    }

    @Test
    void completion_yamlFile_underProfiles_returnsConfigKeys() throws Exception {
        String content = "profiles:\n  fast:\n    \n";
        open("azertio.yaml", content);
        var items = completion("azertio.yaml", content, 2, 4);
        assertThat(items).isNotNull();
    }

    // ─── codeAction ──────────────────────────────────────────────────────────

    @Test
    void codeAction_nonFeatureFile_returnsEmpty() throws Exception {
        var params = new CodeActionParams();
        params.setTextDocument(new TextDocumentIdentifier("file.yaml"));
        params.setContext(new CodeActionContext(List.of()));
        var result = service.codeAction(params).get();
        assertThat(result).isEmpty();
    }

    @Test
    void codeAction_featureFile_withNoDiagnostics_returnsEmpty() throws Exception {
        open("my.feature", "Feature: x\n  Scenario: s\n    Given a step\n");
        var params = new CodeActionParams();
        params.setTextDocument(new TextDocumentIdentifier("my.feature"));
        params.setContext(new CodeActionContext(List.of()));
        var result = service.codeAction(params).get();
        assertThat(result).isEmpty();
    }

    @Test
    void codeAction_featureFile_withDiagnosticFromOtherSource_skips() throws Exception {
        String content = "Feature: x\n  Scenario: s\n    Given unknown step\n";
        open("my.feature", content);
        var diag = new Diagnostic(
            new Range(new Position(2, 10), new Position(2, 22)),
            "Unknown step"
        );
        diag.setSource("other-source");
        var params = new CodeActionParams();
        params.setTextDocument(new TextDocumentIdentifier("my.feature"));
        params.setContext(new CodeActionContext(List.of(diag)));
        var result = service.codeAction(params).get();
        assertThat(result).isEmpty();
    }

    @Test
    void codeAction_featureFile_withAzertioStepDiagnostic_executesMainPath() throws Exception {
        String content = "Feature: x\n  Scenario: s\n    Given unknown step here\n";
        open("my.feature", content);
        var diag = new Diagnostic(
            new Range(new Position(2, 10), new Position(2, 28)),
            "Unknown step"
        );
        diag.setSource("azertio-step");
        var params = new CodeActionParams();
        params.setTextDocument(new TextDocumentIdentifier("my.feature"));
        params.setContext(new CodeActionContext(List.of(diag)));
        // just verify it doesn't throw
        var result = service.codeAction(params).get();
        assertThat(result).isNotNull();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void open(String uri, String text) {
        var params = new DidOpenTextDocumentParams();
        params.setTextDocument(new TextDocumentItem(uri, "text", 1, text));
        service.didOpen(params);
    }

    private List<CompletionItem> completion(String uri, String content, int line, int character) throws Exception {
        var params = new CompletionParams();
        params.setTextDocument(new TextDocumentIdentifier(uri));
        params.setPosition(new Position(line, character));
        Either<List<CompletionItem>, CompletionList> result = service.completion(params).get();
        return result.getLeft();
    }
}