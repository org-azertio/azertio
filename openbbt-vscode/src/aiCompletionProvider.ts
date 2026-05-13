import * as vscode from 'vscode';
import { OpenBBTClient } from './openbbtClient';

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

export class AiCompletionProvider implements vscode.InlineCompletionItemProvider {

    constructor(
        private readonly getClient: () => OpenBBTClient | undefined,
        private readonly statusBar: vscode.StatusBarItem
    ) {}

    async provideInlineCompletionItems(
        document: vscode.TextDocument,
        position: vscode.Position,
        _context: vscode.InlineCompletionContext,
        token: vscode.CancellationToken
    ): Promise<vscode.InlineCompletionList | undefined> {
        const config = vscode.workspace.getConfiguration('openbbt.ai');
        if (!config.get<boolean>('enabled', false)) { return; }

        if (!isOnStepLine(document.lineAt(position.line).text)) { return; }

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

        if (token.isCancellationRequested) { return; }

        const modelFamily = config.get<string>('model', '');

        this.statusBar.text = '$(loading~spin) OpenBBT AI';
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
        const selector: vscode.LanguageModelChatSelector = modelFamily ? { family: modelFamily } : {};
        const models = await vscode.lm.selectChatModels(selector);
        if (models.length === 0) { return undefined; }

        const prompt =
            `You are an assistant for writing BDD tests in Gherkin.\n` +
            `Active language: ${locale}\n` +
            `Available steps in this project:\n<steps>\n${stepsIndex}\n</steps>\n` +
            `Suggest the text that completes the current step from the cursor position onward.\n` +
            `Return ONLY the completion text. No explanations, no quotes.\n` +
            `If no step fits, return an empty string.\n\n` +
            `File content up to cursor:\n${prefix}`;

        try {
            const response = await models[0].sendRequest(
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
        } catch {
            return undefined;
        }
    }
}
