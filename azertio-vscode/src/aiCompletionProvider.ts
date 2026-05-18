import * as vscode from 'vscode';
import { AzertioClient } from './azertioClient';

const STEP_PREFIXES = [
    'given ', 'when ', 'then ', 'and ', 'but ', '* ',
    'dado que ', 'dado ', 'cuando ', 'entonces ', 'y ', 'pero ',
];

function localeFromDocument(doc: vscode.TextDocument): string {
    for (let i = 0; i < Math.min(5, doc.lineCount); i++) {
        const m = doc.lineAt(i).text.trim().match(/^#\s*language\s*:\s*(\w+)/i);
        if (m) { return m[1]; }
    }
    return 'en';
}

function isOnStepLine(lineText: string): boolean {
    const lower = lineText.trimStart().toLowerCase();
    return STEP_PREFIXES.some(p => lower.startsWith(p));
}

const DEBOUNCE_MS = 600;

function localesFromIndex(stepsIndex: string): string[] {
    try {
        const steps = JSON.parse(stepsIndex);
        if (Array.isArray(steps) && steps.length > 0 && steps[0].expressions) {
            return Object.keys(steps[0].expressions);
        }
    } catch { /* ignore */ }
    return ['en'];
}

async function selectModel(modelFamily: string): Promise<vscode.LanguageModelChat | undefined> {
    const selector: vscode.LanguageModelChatSelector = modelFamily
        ? { family: modelFamily }
        : { vendor: 'copilot' };
    let models = await vscode.lm.selectChatModels(selector);
    if (models.length === 0 && !modelFamily) {
        models = await vscode.lm.selectChatModels({});
    }
    return models[0];
}

export async function generateFeature(
    document: vscode.TextDocument,
    getClient: () => AzertioClient | undefined,
    statusBar: vscode.StatusBarItem,
    log: (msg: string) => void
): Promise<void> {
    const config = vscode.workspace.getConfiguration('azertio.ai');
    if (!config.get<boolean>('enabled', false)) {
        vscode.window.showWarningMessage(
            'Azertio AI is disabled. Enable it with "Azertio: Toggle AI Completions".'
        );
        return;
    }

    if (document.getText().trim().length > 0) {
        const answer = await vscode.window.showWarningMessage(
            'The file is not empty. Replace its content with the generated feature?',
            'Replace', 'Cancel'
        );
        if (answer !== 'Replace') { return; }
    }

    let stepsIndex = '[]';
    const client = getClient();
    if (client) {
        try { stepsIndex = await client.getStepsIndex(); } catch { /* sin índice */ }
    }

    const availableLocales = localesFromIndex(stepsIndex);
    const detectedLocale = localeFromDocument(document);
    const pickedLocale = await vscode.window.showQuickPick(
        availableLocales.map(l => ({ label: l, picked: l === detectedLocale })),
        { title: 'Feature language', placeHolder: 'Select a language' }
    );
    if (!pickedLocale) { return; }
    const locale = pickedLocale.label;

    const topic = await vscode.window.showInputBox({
        prompt: 'What is this feature about?',
        placeHolder: 'e.g. query pets by species'
    });
    if (!topic) { return; }

    const modelFamily = config.get<string>('model', '');

    await vscode.window.withProgress(
        { location: vscode.ProgressLocation.Notification, title: 'Azertio AI', cancellable: true },
        async (progress, progressToken) => {
            const cts = new vscode.CancellationTokenSource();
            progressToken.onCancellationRequested(() => cts.cancel());
            try {
                progress.report({ message: 'Generating feature...' });

                const model = await selectModel(modelFamily);
                if (!model) {
                    log('AI: no language model available');
                    vscode.window.showWarningMessage(
                        'Azertio AI: no language model available. Make sure GitHub Copilot Chat is installed and signed in.'
                    );
                    return;
                }

                const keywords = gherkinKeywordsHint(locale);
                const keywordsHint = keywords
                    ? `You MUST use ONLY these exact Gherkin structural keywords for locale "${locale}" (do NOT use English keywords or any other alias):\n${keywords}\n`
                    : '';

                const prompt =
                    `You are an assistant for writing BDD tests in Gherkin format.\n` +
                    `Active language: ${locale}\n` +
                    keywordsHint +
                    `Available steps in this project:\n<steps>\n${stepsIndex}\n</steps>\n` +
                    `Generate a complete Gherkin feature file about: "${topic}".\n` +
                    `Use ONLY the steps listed above, using their "${locale}" expression variant.\n` +
                    `Include a Feature description and 2-3 Scenarios.\n` +
                    `Do NOT include a "# language:" header line — it will be added automatically.\n` +
                    `Return ONLY the Gherkin content. No explanations, no markdown code blocks.`;

                const response = await model.sendRequest(
                    [vscode.LanguageModelChatMessage.User(prompt)],
                    {},
                    cts.token
                );

                let text = '';
                for await (const chunk of response.text) {
                    if (cts.token.isCancellationRequested) { return; }
                    text += chunk;
                }
                if (!text.trim()) { return; }

                text = normalizeGherkinKeywords(text, locale);
                const header = locale !== 'en' ? `# language: ${locale}\n` : '';
                const edit = new vscode.WorkspaceEdit();
                const fullRange = new vscode.Range(
                    document.positionAt(0),
                    document.positionAt(document.getText().length)
                );
                edit.replace(document.uri, fullRange, header + text.trim());
                await vscode.workspace.applyEdit(edit);
            } catch (err) {
                log(`AI: generateFeature error — ${err}`);
            } finally {
                cts.dispose();
            }
        }
    );
}

interface OpenApiEndpoint {
    method: string;
    path: string;
    summary?: string;
    description?: string;
    parameters?: Array<{ name: string; in: string; required?: boolean }>;
}
// canonical scenario keyword and aliases that the model might use instead, per locale
const SCENARIO_KEYWORDS: Record<string, { canonical: string; aliases: string[] }> = {
    'af': { canonical: 'Situasie',    aliases: ['Voorbeeld'] },
    'ar': { canonical: 'سيناريو',     aliases: ['مثال'] },
    'cs': { canonical: 'Scénář',      aliases: ['Příklad'] },
    'cy-GB': { canonical: 'Scenario', aliases: ['Enghraifft'] },
    'da': { canonical: 'Scenarie',    aliases: ['Eksempel'] },
    'de': { canonical: 'Szenario',    aliases: ['Beispiel'] },
    'el': { canonical: 'Σενάριο',     aliases: ['Παράδειγμα'] },
    'en': { canonical: 'Scenario',    aliases: ['Example'] },
    'en-Scouse': { canonical: 'Scenario', aliases: ['Example'] },
    'eo': { canonical: 'Scenaro',     aliases: ['Ekzemplo'] },
    'es': { canonical: 'Escenario',   aliases: ['Ejemplo'] },
    'et': { canonical: 'Stsenaarium', aliases: ['Näide'] },
    'fi': { canonical: 'Skenaario',   aliases: ['Esimerkki'] },
    'fr': { canonical: 'Scénario',    aliases: ['Exemple'] },
    'ga': { canonical: 'Cás',         aliases: ['Sampla'] },
    'gl': { canonical: 'Escenario',   aliases: ['Exemplo'] },
    'he': { canonical: 'תרחיש',       aliases: ['דוגמא'] },
    'hr': { canonical: 'Scenarij',    aliases: ['Primjer'] },
    'hu': { canonical: 'Forgatókönyv', aliases: ['Példa'] },
    'id': { canonical: 'Skenario',    aliases: ['Contoh', 'Misal'] },
    'is': { canonical: 'Atburðarás',  aliases: ['Dæmi'] },
    'it': { canonical: 'Scenario',    aliases: ['Esempio'] },
    'ja': { canonical: 'シナリオ',    aliases: ['例'] },
    'ka': { canonical: 'სცენარი',     aliases: ['მაგალითი'] },
    'kn': { canonical: 'ಕಥಾಸಾರಾಂಶ',  aliases: ['ಉದಾಹರಣೆ'] },
    'ko': { canonical: '시나리오',    aliases: ['예시'] },
    'lt': { canonical: 'Scenarijus',  aliases: ['Pavyzdys'] },
    'lv': { canonical: 'Scenārijs',   aliases: ['Piemērs'] },
    'nl': { canonical: 'Scenario',    aliases: ['Voorbeeld'] },
    'no': { canonical: 'Scenario',    aliases: ['Eksempel'] },
    'pa': { canonical: 'ਦ੍ਰਿਸ਼',     aliases: ['ਉਦਾਹਰਨ'] },
    'pl': { canonical: 'Scenariusz',  aliases: ['Przykład'] },
    'pt': { canonical: 'Cenário',     aliases: ['Exemplo'] },
    'ro': { canonical: 'Scenariu',    aliases: ['Exemplu'] },
    'ru': { canonical: 'Сценарий',    aliases: ['Пример'] },
    'sk': { canonical: 'Scenár',      aliases: ['Príklad'] },
    'sl': { canonical: 'Scenarij',    aliases: ['Primer'] },
    'sr-Cyrl': { canonical: 'Сценарио', aliases: ['Пример'] },
    'sr-Latn': { canonical: 'Scenario', aliases: ['Primer'] },
    'sv': { canonical: 'Scenario',    aliases: ['Exempel'] },
    'th': { canonical: 'เหตุการณ์',  aliases: ['ตัวอย่าง'] },
    'tl': { canonical: 'Senaryo',     aliases: ['Halimbawa'] },
    'tr': { canonical: 'Senaryo',     aliases: ['Örnek'] },
    'tt': { canonical: 'Сценарий',    aliases: ['Мисал'] },
    'uk': { canonical: 'Сценарій',    aliases: ['Приклад'] },
    'ur': { canonical: 'منظرنامہ',    aliases: ['مثال'] },
    'uz': { canonical: 'Stsenariy',   aliases: ['Misol'] },
    'vi': { canonical: 'Tình huống',  aliases: ['Ví dụ'] },
    'zh-CN': { canonical: '场景',     aliases: ['例子'] },
    'zh-TW': { canonical: '場景',     aliases: ['例子'] },
};

function escapeRegex(s: string): string {
    return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function gherkinKeywordsHint(locale: string): string | undefined {
    const entry = SCENARIO_KEYWORDS[locale];
    if (!entry) { return undefined; }
    return (
        `- Scenario blocks → "${entry.canonical}" ` +
        `(NEVER use ${entry.aliases.map(a => `"${a}"`).join(' or ')})`
    );
}

function normalizeGherkinKeywords(text: string, locale: string): string {
    const entry = SCENARIO_KEYWORDS[locale];
    if (!entry) { return text; }
    let inDocString = false;
    const pattern = new RegExp(
        `^(\\s*)(${entry.aliases.map(escapeRegex).join('|')})(\\s*:)`, 'i'
    );
    return text.split('\n').map(line => {
        if (line.trim().startsWith('"""') || line.trim().startsWith('```')) {
            inDocString = !inDocString;
            return line;
        }
        if (inDocString) { return line; }
        return line.replace(pattern, `$1${entry.canonical}$3`);
    }).join('\n');
}

async function fetchOpenApiSpec(urlOrPath: string): Promise<any> {
    if (urlOrPath.startsWith('http://') || urlOrPath.startsWith('https://')) {
        const res = await fetch(urlOrPath);
        if (!res.ok) { throw new Error(`HTTP ${res.status} fetching ${urlOrPath}`); }
        return res.json();
    }
    const bytes = await vscode.workspace.fs.readFile(vscode.Uri.file(urlOrPath));
    return JSON.parse(new TextDecoder().decode(bytes));
}

function parseEndpoints(spec: any): OpenApiEndpoint[] {
    const endpoints: OpenApiEndpoint[] = [];
    const httpMethods = ['get', 'post', 'put', 'patch', 'delete', 'head', 'options'];
    for (const [path, pathItem] of Object.entries((spec.paths ?? {}) as Record<string, any>)) {
        for (const method of httpMethods) {
            const op = pathItem[method];
            if (!op) { continue; }
            endpoints.push({
                method: method.toUpperCase(),
                path,
                summary: op.summary,
                description: op.description,
                parameters: op.parameters,
            });
        }
    }
    return endpoints;
}

function endpointsToContext(endpoints: OpenApiEndpoint[]): string {
    return endpoints.map(e => {
        let line = `${e.method} ${e.path}`;
        if (e.summary) { line += ` — ${e.summary}`; }
        if (e.parameters?.length) {
            const params = e.parameters.map(p => `${p.in}:${p.name}${p.required ? '*' : ''}`).join(', ');
            line += ` [${params}]`;
        }
        return line;
    }).join('\n');
}

export async function generateFeatureFromSwagger(
    document: vscode.TextDocument,
    getClient: () => AzertioClient | undefined,
    statusBar: vscode.StatusBarItem,
    log: (msg: string) => void
): Promise<void> {
    const config = vscode.workspace.getConfiguration('azertio.ai');
    if (!config.get<boolean>('enabled', false)) {
        vscode.window.showWarningMessage(
            'Azertio AI is disabled. Enable it with "Azertio: Toggle AI Completions".'
        );
        return;
    }

    if (document.getText().trim().length > 0) {
        const answer = await vscode.window.showWarningMessage(
            'The file is not empty. Replace its content with the generated feature?',
            'Replace', 'Cancel'
        );
        if (answer !== 'Replace') { return; }
    }

    const swaggerUrl = await vscode.window.showInputBox({
        prompt: 'OpenAPI/Swagger JSON spec URL or local file path',
        placeHolder: 'e.g. http://localhost:8080/v3/api-docs',
    });
    if (!swaggerUrl) { return; }

    statusBar.text = '$(loading~spin) Azertio AI';
    statusBar.show();

    let spec: any;
    try {
        spec = await fetchOpenApiSpec(swaggerUrl);
    } catch (err) {
        statusBar.hide();
        log(`AI: error fetching swagger spec — ${err}`);
        vscode.window.showErrorMessage(`Azertio AI: could not fetch spec — ${err}`);
        return;
    }

    const endpoints = parseEndpoints(spec);
    if (endpoints.length === 0) {
        statusBar.hide();
        vscode.window.showWarningMessage('Azertio AI: no endpoints found in the spec.');
        return;
    }

    let stepsIndex = '[]';
    const client = getClient();
    if (client) {
        try { stepsIndex = await client.getStepsIndex(); } catch { /* no index */ }
    }

    const availableLocales = localesFromIndex(stepsIndex);
    const detectedLocale = localeFromDocument(document);
    const pickedLocale = await vscode.window.showQuickPick(
        availableLocales.map(l => ({ label: l, picked: l === detectedLocale })),
        { title: 'Feature language', placeHolder: 'Select a language' }
    );
    if (!pickedLocale) { return; }
    const locale = pickedLocale.label;

    const modelFamily = config.get<string>('model', '');

    await vscode.window.withProgress(
        { location: vscode.ProgressLocation.Notification, title: 'Azertio AI', cancellable: true },
        async (progress, progressToken) => {
            const cts = new vscode.CancellationTokenSource();
            progressToken.onCancellationRequested(() => cts.cancel());
            try {
                progress.report({ message: 'Generating feature...' });

                const model = await selectModel(modelFamily);
                if (!model) {
                    log('AI: no language model available');
                    vscode.window.showWarningMessage(
                        'Azertio AI: no language model available. Make sure GitHub Copilot Chat is installed and signed in.'
                    );
                    return;
                }

                const keywords = gherkinKeywordsHint(locale);
                const keywordsHint = keywords
                    ? `You MUST use ONLY these exact Gherkin structural keywords for locale "${locale}" (do NOT use English keywords or any other alias):\n${keywords}\n`
                    : '';

                const apiTitle = spec.info?.title ?? 'the API';
                const prompt =
                    `You are an assistant for writing BDD tests in Gherkin format.\n` +
                    `Active language: ${locale}\n` +
                    keywordsHint +
                    `Available steps in this project:\n<steps>\n${stepsIndex}\n</steps>\n` +
                    `API: ${apiTitle}\n` +
                    `Endpoints:\n${endpointsToContext(endpoints)}\n\n` +
                    `Generate a complete Gherkin feature file covering these API endpoints.\n` +
                    `Use ONLY the steps listed above, using their "${locale}" expression variant.\n` +
                    `Group related endpoints into Scenarios. Include a Feature description.\n` +
                    `Do NOT include a "# language:" header line — it will be added automatically.\n` +
                    `Return ONLY the Gherkin content. No explanations, no markdown code blocks.`;

                const response = await model.sendRequest(
                    [vscode.LanguageModelChatMessage.User(prompt)],
                    {},
                    cts.token
                );

                let text = '';
                for await (const chunk of response.text) {
                    if (cts.token.isCancellationRequested) { return; }
                    text += chunk;
                }
                if (!text.trim()) { return; }

                text = normalizeGherkinKeywords(text, locale);
                const header = locale !== 'en' ? `# language: ${locale}\n` : '';
                const edit = new vscode.WorkspaceEdit();
                const fullRange = new vscode.Range(
                    document.positionAt(0),
                    document.positionAt(document.getText().length)
                );
                edit.replace(document.uri, fullRange, header + text.trim());
                await vscode.workspace.applyEdit(edit);
            } catch (err) {
                log(`AI: generateFeatureFromSwagger error — ${err}`);
            } finally {
                cts.dispose();
            }
        }
    );
}

export class AiFeatureCodeLensProvider implements vscode.CodeLensProvider {
    provideCodeLenses(document: vscode.TextDocument): vscode.CodeLens[] {
        if (document.getText().trim().length > 0) { return []; }
        return [
            new vscode.CodeLens(new vscode.Range(0, 0, 0, 0), {
                title: '$(sparkle) Generate feature with AI',
                command: 'azertio.ai.generateFeature'
            }),
            new vscode.CodeLens(new vscode.Range(0, 0, 0, 0), {
                title: '$(cloud-download) Generate feature from Swagger',
                command: 'azertio.ai.generateFeatureFromSwagger'
            })
        ];
    }
}

export class AiCompletionProvider implements vscode.InlineCompletionItemProvider {

    private lastCallId = 0;

    constructor(
        private readonly getClient: () => AzertioClient | undefined,
        private readonly statusBar: vscode.StatusBarItem,
        private readonly log: (msg: string) => void
    ) {}

    async provideInlineCompletionItems(
        document: vscode.TextDocument,
        position: vscode.Position,
        _context: vscode.InlineCompletionContext,
        token: vscode.CancellationToken
    ): Promise<vscode.InlineCompletionList | undefined> {
        const config = vscode.workspace.getConfiguration('azertio.ai');
        if (!config.get<boolean>('enabled', false)) { return; }

        if (!isOnStepLine(document.lineAt(position.line).text)) { return; }

        const callId = ++this.lastCallId;
        await new Promise<void>(resolve => setTimeout(resolve, DEBOUNCE_MS));
        if (callId !== this.lastCallId || token.isCancellationRequested) { return; }

        const locale = localeFromDocument(document);
        const prefix = document.getText(new vscode.Range(new vscode.Position(0, 0), position));

        let stepsIndex = '[]';
        const client = this.getClient();
        if (client) {
            try {
                stepsIndex = await client.getStepsIndex();
            } catch {
                // serve not available — proceed without index
            }
        }

        if (callId !== this.lastCallId || token.isCancellationRequested) { return; }

        const modelFamily = config.get<string>('model', '');

        this.statusBar.text = '$(loading~spin) Azertio AI';
        this.statusBar.show();
        try {
            const completion = await this.callModel(modelFamily, locale, stepsIndex, prefix, token);
            if (!completion || token.isCancellationRequested) { return; }

            const endOfLine = document.lineAt(position.line).range.end;
            return new vscode.InlineCompletionList([
                new vscode.InlineCompletionItem(
                    completion,
                    new vscode.Range(position, endOfLine)
                )
            ]);
        } finally {
            this.statusBar.hide();
        }
    }

    private async callModel(
        modelFamily: string,
        locale: string,
        stepsIndex: string,
        prefix: string,
        token: vscode.CancellationToken
    ): Promise<string | undefined> {
        const model = await selectModel(modelFamily);
        if (!model) {
            this.log('AI: no language model available — check Copilot Chat is installed and signed in');
            vscode.window.showWarningMessage(
                'Azertio AI: no language model available. Make sure GitHub Copilot Chat is installed and signed in.'
            );
            return undefined;
        }

        const prompt =
            `You are an assistant for writing BDD tests in Gherkin.\n` +
            `Active language: ${locale}\n` +
            `Available steps in this project:\n<steps>\n${stepsIndex}\n</steps>\n` +
            `Suggest the text that completes the current step from the cursor position onward.\n` +
            `Return ONLY the completion text. No explanations, no quotes.\n` +
            `If no step fits, return an empty string.\n\n` +
            `File content up to cursor:\n${prefix}`;

        try {
            const response = await model.sendRequest(
                [vscode.LanguageModelChatMessage.User(prompt)],
                {},
                token
            );
            let text = '';
            for await (const chunk of response.text) {
                if (token.isCancellationRequested) { return undefined; }
                text += chunk;
            }
            return text.trim() || undefined;
        } catch (err) {
            this.log(`AI: sendRequest error — ${err}`);
            return undefined;
        }
    }
}
