package org.azertio.lsp.test;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.azertio.lsp.AzertioTextDocumentService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class AzertioTextDocumentServiceTest {

    private AzertioTextDocumentService service;
    private final List<PublishDiagnosticsParams> published = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // null backend → featureDiagnostics disabled, null config → no config key completions
        service = new AzertioTextDocumentService(null, new DefaultKeywordMapProvider(), null);
        service.setClient(stubClient());
    }

    // ─── didOpen ──────────────────────────────────────────────────────────────

    @Test
    void didOpen_validYaml_publishesNoDiagnostics() {
        open("azertio.yaml", """
            project:
              name: MyProject
              organization: MyOrg
            """);
        assertThat(published).hasSize(1);
        assertThat(published.get(0).getDiagnostics()).isEmpty();
    }

    @Test
    void didOpen_invalidYaml_publishesDiagnostics() {
        open("azertio.yaml", "project:\n  name: unclosed {");
        assertThat(published).hasSize(1);
        assertThat(published.get(0).getDiagnostics()).isNotEmpty();
    }

    @Test
    void didOpen_featureFile_withNullBackend_doesNotPublish() {
        open("my.feature", "Feature: x\n  Scenario: y\n    Given a step\n");
        // featureDiagnostics is null (null backend) → publishDiagnostics returns early
        assertThat(published).isEmpty();
    }

    @Test
    void didOpen_unknownFileType_doesNotPublish() {
        open("README.md", "# hello");
        assertThat(published).isEmpty();
    }

    @Test
    void didOpen_withNullClient_doesNotThrow() {
        service = new AzertioTextDocumentService(null, new DefaultKeywordMapProvider(), null);
        // no client set → publishDiagnostics returns early without NPE
        open("azertio.yaml", "project:\n  name: MyProject\n  organization: MyOrg\n");
        assertThat(published).isEmpty(); // published list of THIS test's client is unaffected
    }

    // ─── didChange ────────────────────────────────────────────────────────────

    @Test
    void didChange_withNewYamlContent_publishesDiagnostics() {
        open("azertio.yaml", "project:\n  name: P\n  organization: O\n");
        published.clear();

        var params = new DidChangeTextDocumentParams();
        params.setTextDocument(new VersionedTextDocumentIdentifier("azertio.yaml", 2));
        var change = new TextDocumentContentChangeEvent();
        change.setText("plugins:\n  - missing-project\n");
        params.setContentChanges(List.of(change));
        service.didChange(params);

        assertThat(published).hasSize(1);
        assertThat(published.get(0).getDiagnostics()).isNotEmpty();
    }

    @Test
    void didChange_withNoChanges_doesNotPublish() {
        var params = new DidChangeTextDocumentParams();
        params.setTextDocument(new VersionedTextDocumentIdentifier("azertio.yaml", 1));
        params.setContentChanges(List.of());
        service.didChange(params);
        assertThat(published).isEmpty();
    }

    // ─── didClose ─────────────────────────────────────────────────────────────

    @Test
    void didClose_publishesEmptyDiagnostics() {
        open("azertio.yaml", "project:\n  name: P\n  organization: O\n");
        published.clear();

        var params = new DidCloseTextDocumentParams();
        params.setTextDocument(new TextDocumentIdentifier("azertio.yaml"));
        service.didClose(params);

        assertThat(published).hasSize(1);
        assertThat(published.get(0).getDiagnostics()).isEmpty();
    }

    // ─── didSave ──────────────────────────────────────────────────────────────

    @Test
    void didSave_isNoOp() {
        var params = new DidSaveTextDocumentParams();
        params.setTextDocument(new TextDocumentIdentifier("azertio.yaml"));
        service.didSave(params);
        assertThat(published).isEmpty();
    }

    // ─── codeAction ───────────────────────────────────────────────────────────

    @Test
    void codeAction_featureFile_nullBackend_returnsEmpty() throws Exception {
        var params = new CodeActionParams();
        params.setTextDocument(new TextDocumentIdentifier("my.feature"));
        params.setContext(new CodeActionContext(List.of()));
        var result = service.codeAction(params).get();
        assertThat(result).isEmpty();
    }

    @Test
    void codeAction_nonFeatureFile_returnsEmpty() throws Exception {
        var params = new CodeActionParams();
        params.setTextDocument(new TextDocumentIdentifier("azertio.yaml"));
        params.setContext(new CodeActionContext(List.of()));
        var result = service.codeAction(params).get();
        assertThat(result).isEmpty();
    }

    // ─── completion: YAML ─────────────────────────────────────────────────────

    @Test
    void completion_yamlFile_atRootLevel_returnsRootKeys() throws Exception {
        open("azertio.yaml", "\n");
        var result = completion("azertio.yaml", "", 0, 0);
        assertThat(result).isNotEmpty();
        assertThat(result.stream().map(CompletionItem::getLabel))
            .contains("project:", "plugins:", "configuration:", "profiles:");
    }

    @Test
    void completion_yamlFile_underProjectKey_returnsProjectKeys() throws Exception {
        String content = "project:\n  \n";
        open("azertio.yaml", content);
        var result = completion("azertio.yaml", content, 1, 2);
        assertThat(result).isNotEmpty();
        assertThat(result.stream().map(CompletionItem::getLabel))
            .contains("name:", "organization:");
    }

    @Test
    void completion_yamlFile_underProjectTestSuites_returnsSuiteKeys() throws Exception {
        String content = "project:\n  test-suites:\n    - \n";
        open("azertio.yaml", content);
        var result = completion("azertio.yaml", content, 2, 6);
        assertThat(result).isNotEmpty();
        assertThat(result.stream().map(CompletionItem::getLabel))
            .contains("name:", "description:");
    }

    @Test
    void completion_yamlFile_underConfiguration_withNullConfig_returnsEmpty() throws Exception {
        String content = "configuration:\n  \n";
        open("azertio.yaml", content);
        var result = completion("azertio.yaml", content, 1, 2);
        assertThat(result).isEmpty(); // config is null → configKeyCompletions returns empty
    }

    @Test
    void completion_yamlFile_underProfiles_withNullConfig_returnsEmpty() throws Exception {
        String content = "profiles:\n  fast:\n    \n";
        open("azertio.yaml", content);
        var result = completion("azertio.yaml", content, 2, 4);
        assertThat(result).isEmpty(); // config is null
    }

    @Test
    void completion_yamlFile_withUnknownSection_returnsEmpty() throws Exception {
        String content = "unknown:\n  \n";
        open("azertio.yaml", content);
        var result = completion("azertio.yaml", content, 1, 2);
        assertThat(result).isEmpty();
    }

    @Test
    void completion_yamlFile_filtersByPrefix() throws Exception {
        String content = "pr\n";
        open("azertio.yaml", content);
        var result = completion("azertio.yaml", content, 0, 2);
        assertThat(result).isNotEmpty();
        assertThat(result.stream().map(CompletionItem::getLabel))
            .allMatch(label -> label.toLowerCase().startsWith("pr"));
    }

    // ─── completion: feature ─────────────────────────────────────────────────

    @Test
    void completion_featureFile_atBlankLine_returnsGherkinKeywords() throws Exception {
        String content = "Feature: x\n  \n";
        open("my.feature", content);
        var result = completion("my.feature", content, 1, 2);
        assertThat(result).isNotEmpty();
    }

    @Test
    void completion_featureFile_atStepKeyword_withNullBackend_returnsEmpty() throws Exception {
        String content = "Feature: x\n  Scenario: s\n    Given \n";
        open("my.feature", content);
        var result = completion("my.feature", content, 2, 10);
        // stepCompletions returns empty because backend is null
        assertThat(result).isEmpty();
    }

    @Test
    void completion_featureFile_atCommentLine_withNullConfig_returnsEmpty() throws Exception {
        String content = "Feature: x\n  Scenario: s\n    # \n";
        open("my.feature", content);
        var result = completion("my.feature", content, 2, 7);
        // configFlatKeyCompletions returns empty because config is null
        assertThat(result).isEmpty();
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
        var result = service.completion(params).get();
        return result.getLeft();
    }

    private LanguageClient stubClient() {
        return new LanguageClient() {
            @Override public void telemetryEvent(Object object) {}
            @Override public void publishDiagnostics(PublishDiagnosticsParams params) { published.add(params); }
            @Override public void showMessage(MessageParams messageParams) {}
            @Override public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
                return CompletableFuture.completedFuture(null);
            }
            @Override public void logMessage(MessageParams message) {}
        };
    }
}