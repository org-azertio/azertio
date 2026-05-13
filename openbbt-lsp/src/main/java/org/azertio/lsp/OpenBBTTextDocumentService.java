package org.azertio.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import java.util.Collections;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.gherkinparser.KeywordType;
import org.myjtools.imconfig.Config;
import org.myjtools.imconfig.PropertyDefinition;
import org.azertio.core.backend.StepProviderBackend;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AzertioTextDocumentService implements TextDocumentService {

    // ─── Gherkin keyword types to offer at structure level ────────────────────

    private static final List<KeywordType> STRUCTURE_KEYWORD_TYPES = List.of(
        KeywordType.FEATURE, KeywordType.BACKGROUND, KeywordType.SCENARIO,
        KeywordType.SCENARIO_OUTLINE, KeywordType.EXAMPLES,
        KeywordType.GIVEN, KeywordType.WHEN, KeywordType.THEN, KeywordType.AND, KeywordType.BUT
    );
    private static final List<KeywordType> STEP_KEYWORD_TYPES = List.of(
        KeywordType.GIVEN, KeywordType.WHEN, KeywordType.THEN, KeywordType.AND, KeywordType.BUT
    );
    private static final List<String> DEFAULT_STEP_KEYWORDS  = List.of("Given", "When", "Then", "And", "But");
    private static final List<String> DEFAULT_GHERKIN_KEYWORDS = List.of(
        "Feature:", "Background:", "Scenario:", "Scenario Outline:",
        "Examples:", "Given ", "When ", "Then ", "And ", "But "
    );

    // ─── azertio.yaml schema (static) ─────────────────────────────────────────

    private static final List<String> ROOT_YAML_KEYS = List.of(
        "project:", "plugins:", "configuration:", "profiles:"
    );
    private static final List<String> PROJECT_KEYS = List.of(
        "name:", "description:", "organization:", "test-suites:"
    );
    private static final List<String> SUITE_KEYS = List.of(
        "name:", "description:", "tag-expression:"
    );

    // ─── State ────────────────────────────────────────────────────────────────

    private final StepProviderBackend backend;
    private final DefaultKeywordMapProvider keywordProvider;
    private final Config config;
    private final YamlDiagnosticsProvider yamlDiagnostics = new YamlDiagnosticsProvider();
    private FeatureDiagnosticsProvider featureDiagnostics;
    private final Map<String, String> documents = new HashMap<>();
    private LanguageClient client;

    public AzertioTextDocumentService(StepProviderBackend backend, DefaultKeywordMapProvider keywordProvider, Config config) {
        this.backend = backend;
        this.keywordProvider = keywordProvider;
        this.config = config;
        if (backend != null) {
            this.featureDiagnostics = new FeatureDiagnosticsProvider(backend, keywordProvider);
        }
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    // ─── TextDocumentService ──────────────────────────────────────────────────

    @Override public void didOpen(DidOpenTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String text = params.getTextDocument().getText();
        documents.put(uri, text);
        publishDiagnostics(uri, text);
    }

    @Override public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        var changes = params.getContentChanges();
        if (!changes.isEmpty()) {
            String text = changes.get(changes.size() - 1).getText();
            documents.put(uri, text);
            publishDiagnostics(uri, text);
        }
    }

    @Override public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        documents.remove(uri);
        if (client != null) {
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, Collections.emptyList()));
        }
    }

    @Override public void didSave(DidSaveTextDocumentParams params) {}

    private void publishDiagnostics(String uri, String content) {
        if (client == null) return;
        List<Diagnostic> diagnostics;
        if (uri.endsWith("azertio.yaml")) {
            diagnostics = yamlDiagnostics.validate(content);
        } else if (uri.endsWith(".feature") && featureDiagnostics != null) {
            diagnostics = featureDiagnostics.validate(content);
        } else {
            return;
        }
        client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        String uri = params.getTextDocument().getUri();
        if (!uri.endsWith(".feature") || featureDiagnostics == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        String content = documents.getOrDefault(uri, "");
        Locale locale = localeFromContent(content);
        String[] lines = content.split("\n", -1);
        var actions = new ArrayList<Either<Command, CodeAction>>();

        for (var diag : params.getContext().getDiagnostics()) {
            if (!FeatureDiagnosticsProvider.SOURCE.equals(diag.getSource())) continue;
            int lineNum = diag.getRange().getStart().getLine();
            if (lineNum >= lines.length) continue;
            String line = lines[lineNum];
            int start = Math.min(diag.getRange().getStart().getCharacter(), line.length());
            int end   = Math.min(diag.getRange().getEnd().getCharacter(), line.length());
            String stepText = line.substring(start, end).stripTrailing();
            if (stepText.isBlank()) continue;

            for (String hint : featureDiagnostics.hintsFor(stepText, locale)) {
                var edit   = new TextEdit(diag.getRange(), hint);
                var action = new CodeAction("Replace with: " + hint);
                action.setKind(CodeActionKind.QuickFix);
                action.setEdit(new WorkspaceEdit(Map.of(uri, List.of(edit))));
                action.setDiagnostics(List.of(diag));
                action.setIsPreferred(actions.isEmpty()); // first hint is preferred
                actions.add(Either.forRight(action));
            }
        }
        return CompletableFuture.completedFuture(actions);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        String uri = params.getTextDocument().getUri();
        String content = documents.getOrDefault(uri, "");
        Position pos = params.getPosition();

        List<CompletionItem> items = uri.endsWith("azertio.yaml")
            ? yamlCompletions(content, pos)
            : featureCompletions(content, pos);

        return CompletableFuture.completedFuture(Either.forLeft(items));
    }

    // ─── Feature file completion ───────────────────────────────────────────────

    private List<CompletionItem> featureCompletions(String content, Position pos) {
        Locale locale = localeFromContent(content);
        String linePrefix = getLinePrefix(content, pos);
        String trimmed = linePrefix.stripLeading();
        int indentLen = linePrefix.length() - trimmed.length();

        var matchingStepKeyword = stepKeywordsForLocale(locale).stream()
            .filter(kw -> trimmed.toLowerCase().startsWith((kw + " ").toLowerCase()))
            .findFirst();

        if (matchingStepKeyword.isPresent()) {
            String kw = matchingStepKeyword.get();
            String stepPrefix = trimmed.substring(kw.length() + 1);
            int stepStartCol = indentLen + kw.length() + 1;
            String[] lines = content.split("\n", -1);
            String fullLine = pos.getLine() < lines.length ? lines[pos.getLine()] : "";
            int lineEndCol = Math.max(stepStartCol, fullLine.stripTrailing().length());
            var replaceRange = new Range(
                new Position(pos.getLine(), stepStartCol),
                new Position(pos.getLine(), lineEndCol)
            );
            return stepCompletions(stepPrefix, locale, replaceRange);
        }

        // Comment line → config property completions
        if (trimmed.startsWith("#")) {
            String afterHash = trimmed.substring(1).stripLeading();
            int propStartCol = linePrefix.length() - afterHash.length();
            var replaceRange = new Range(
                new Position(pos.getLine(), propStartCol),
                new Position(pos.getLine(), linePrefix.length())
            );
            return configFlatKeyCompletions(afterHash, replaceRange);
        }

        return gherkinKeywordCompletions(trimmed, locale);
    }

    private List<String> stepKeywordsForLocale(Locale locale) {
        return keywordProvider.keywordMap(locale)
            .map(km -> STEP_KEYWORD_TYPES.stream()
                .map(km::keywords)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList())
            .orElse(DEFAULT_STEP_KEYWORDS);
    }

    private List<CompletionItem> stepCompletions(String prefix, Locale locale, Range replaceRange) {
        if (backend == null) return List.of();
        return backend.allStepsWithLabelForLocale(locale).stream()
            .filter(e -> e.getValue().toLowerCase().contains(prefix.toLowerCase()))
            .map(e -> {
                String step = e.getValue();
                var item = new CompletionItem("[" + e.getKey() + "] " + step);
                item.setKind(CompletionItemKind.Function);
                item.setInsertTextFormat(InsertTextFormat.Snippet);
                item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, toSnippet(step))));
                return item;
            })
            .toList();
    }

    /** Converts step parameters like {@code {name}} to LSP snippet tabstops like {@code ${1:name}}. */
    private static String toSnippet(String stepText) {
        var sb = new StringBuilder();
        var matcher = java.util.regex.Pattern.compile("\\{([^}]+)\\}").matcher(stepText);
        int counter = 1;
        int last = 0;
        while (matcher.find()) {
            sb.append(stepText, last, matcher.start());
            sb.append("${").append(counter++).append(':').append(matcher.group(1)).append('}');
            last = matcher.end();
        }
        sb.append(stepText, last, stepText.length());
        return sb.toString();
    }

    private List<CompletionItem> gherkinKeywordCompletions(String prefix, Locale locale) {
        List<String> keywords = keywordProvider.keywordMap(locale)
            .map(km -> STRUCTURE_KEYWORD_TYPES.stream()
                .map(km::keywords)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList())
            .orElse(DEFAULT_GHERKIN_KEYWORDS);

        return keywords.stream()
            .filter(kw -> kw.toLowerCase().startsWith(prefix.toLowerCase()))
            .map(kw -> {
                var item = new CompletionItem(kw);
                item.setKind(CompletionItemKind.Keyword);
                return item;
            })
            .toList();
    }

    // ─── azertio.yaml completion ───────────────────────────────────────────────

    private List<CompletionItem> yamlCompletions(String content, Position pos) {
        String[] lines = content.split("\n", -1);
        int cursorLine = pos.getLine();

        // Raw prefix on current line (before stripping indentation)
        String linePrefix = getLinePrefix(content, pos);
        String trimmed = linePrefix.stripLeading();
        // Strip leading list marker if present
        if (trimmed.startsWith("- ")) trimmed = trimmed.substring(2).stripLeading();

        List<String> path = buildYamlPath(lines, cursorLine);

        if (path.isEmpty()) {
            return staticCompletions(ROOT_YAML_KEYS, trimmed, CompletionItemKind.Module);
        }

        return switch (path.get(0)) {
            case "project" -> {
                if (path.size() == 1) yield staticCompletions(PROJECT_KEYS, trimmed, CompletionItemKind.Property);
                if ("test-suites".equals(path.get(1))) yield staticCompletions(SUITE_KEYS, trimmed, CompletionItemKind.Property);
                yield List.of();
            }
            case "configuration" -> configKeyCompletions(path.subList(1, path.size()), trimmed);
            case "profiles" -> {
                // profiles.<name>.<configKey> — offer config keys if depth > 1
                List<String> configSubPath = path.size() > 2 ? path.subList(2, path.size()) : List.of();
                yield configKeyCompletions(configSubPath, trimmed);
            }
            default -> List.of();
        };
    }

    /**
     * Offers config key completions from the runtime's property definitions.
     * subPath is the YAML path segments below "configuration:" (e.g. ["core"] when under configuration.core).
     * The method shows the next segment of all dotted keys matching that prefix.
     */
    private List<CompletionItem> configKeyCompletions(List<String> subPath, String linePrefix) {
        if (config == null) return List.of();
        String dotPrefix = subPath.isEmpty() ? "" : String.join(".", subPath) + ".";
        Map<String, PropertyDefinition> definitions = config.getDefinitions();

        // Group matching definitions by their next segment after dotPrefix
        var segmentMap = new LinkedHashMap<String, PropertyDefinition>();
        for (var entry : definitions.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(dotPrefix)) continue;
            String remaining = key.substring(dotPrefix.length());
            String nextSegment = remaining.contains(".")
                ? remaining.substring(0, remaining.indexOf('.'))
                : remaining;
            if (nextSegment.toLowerCase().startsWith(linePrefix.toLowerCase())) {
                segmentMap.putIfAbsent(nextSegment, entry.getValue());
            }
        }

        return segmentMap.entrySet().stream().map(e -> {
            String segment = e.getKey();
            PropertyDefinition def = e.getValue();
            boolean isLeaf = definitions.containsKey(dotPrefix + segment);

            var item = new CompletionItem(segment + ":");
            item.setKind(isLeaf ? CompletionItemKind.Property : CompletionItemKind.Module);
            String detail = def.description();
            if (isLeaf) {
                String hint = def.defaultValue().map(dv -> detail + " [default: " + dv + "]").orElse(detail);
                item.setDetail(hint);
                if (def.required()) item.setDetail(item.getDetail() + " (required)");
            } else {
                item.setDetail(detail);
            }
            return item;
        }).toList();
    }

    /**
     * Flat config-key completions for feature file comment lines.
     * Offers full dotted keys (e.g. {@code core.resourceFilter: }) that match the given prefix.
     */
    private List<CompletionItem> configFlatKeyCompletions(String prefix, Range replaceRange) {
        if (config == null) return List.of();
        return config.getDefinitions().entrySet().stream()
            .filter(e -> e.getKey().toLowerCase().startsWith(prefix.toLowerCase()))
            .map(e -> {
                String key = e.getKey();
                PropertyDefinition def = e.getValue();
                var item = new CompletionItem(key + ": ");
                item.setKind(CompletionItemKind.Property);
                item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, key + ": ")));
                String detail = def.description();
                String hint = def.defaultValue()
                    .map(dv -> detail + " [default: " + dv + "]")
                    .orElse(detail);
                if (def.required()) hint += " (required)";
                item.setDetail(hint);
                return item;
            })
            .toList();
    }

    private List<CompletionItem> staticCompletions(List<String> keys, String prefix, CompletionItemKind kind) {
        return keys.stream()
            .filter(k -> k.toLowerCase().startsWith(prefix.toLowerCase()))
            .map(k -> {
                var item = new CompletionItem(k);
                item.setKind(kind);
                return item;
            })
            .toList();
    }

    // ─── YAML path builder ────────────────────────────────────────────────────

    /**
     * Builds the logical YAML key path from root to the cursor position.
     * List markers (- ) are consumed but not added to the path.
     *
     * Example: cursor on line with indent 4 under "configuration: / core:" → ["configuration","core"]
     */
    private List<String> buildYamlPath(String[] lines, int cursorLine) {
        var path = new LinkedList<String>();
        int targetIndent = lineIndent(lines[cursorLine]);

        for (int i = cursorLine - 1; i >= 0 && targetIndent > 0; i--) {
            String line = lines[i];
            if (line.isBlank()) continue;
            int indent = lineIndent(line);
            if (indent >= targetIndent) continue;

            String trimmed = line.stripLeading();
            if (trimmed.startsWith("- ") || trimmed.equals("-")) {
                // List marker: update target indent but don't add a key
                targetIndent = indent;
            } else {
                String key = extractYamlKey(trimmed);
                if (key != null) {
                    path.addFirst(key);
                    targetIndent = indent;
                }
            }
        }
        return path;
    }

    private int lineIndent(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        return i;
    }

    private String extractYamlKey(String trimmedLine) {
        // Strip list marker
        if (trimmedLine.startsWith("- ")) trimmedLine = trimmedLine.substring(2);
        int colon = trimmedLine.indexOf(':');
        if (colon > 0) return trimmedLine.substring(0, colon).trim();
        return null;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String getLinePrefix(String content, Position pos) {
        String[] lines = content.split("\n", -1);
        if (pos.getLine() >= lines.length) return "";
        String line = lines[pos.getLine()];
        int col = Math.min(pos.getCharacter(), line.length());
        return line.substring(0, col);
    }

    private Locale localeFromContent(String content) {
        for (String line : content.split("\n", 5)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# language:")) {
                return Locale.forLanguageTag(trimmed.substring("# language:".length()).trim());
            }
        }
        return Locale.getDefault();
    }
}
