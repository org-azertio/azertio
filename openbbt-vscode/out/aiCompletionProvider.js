"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.AiCompletionProvider = void 0;
const vscode = __importStar(require("vscode"));
const STEP_PREFIXES = [
    'given ', 'when ', 'then ', 'and ', 'but ', '* ',
    'dado que ', 'dado ', 'cuando ', 'entonces ', 'y ', 'pero ',
];
function localeFromDocument(doc) {
    for (let i = 0; i < Math.min(5, doc.lineCount); i++) {
        const m = doc.lineAt(i).text.trim().match(/^#\s*language\s*:\s*(\w+)/i);
        if (m) {
            return m[1];
        }
    }
    return 'en';
}
function isOnStepLine(lineText) {
    const lower = lineText.trimStart().toLowerCase();
    return STEP_PREFIXES.some(p => lower.startsWith(p));
}
class AiCompletionProvider {
    getClient;
    statusBar;
    constructor(getClient, statusBar) {
        this.getClient = getClient;
        this.statusBar = statusBar;
    }
    async provideInlineCompletionItems(document, position, _context, token) {
        const config = vscode.workspace.getConfiguration('openbbt.ai');
        if (!config.get('enabled', false)) {
            return;
        }
        if (!isOnStepLine(document.lineAt(position.line).text)) {
            return;
        }
        const locale = localeFromDocument(document);
        const prefix = document.getText(new vscode.Range(new vscode.Position(0, 0), position));
        let stepsIndex = '[]';
        const client = this.getClient();
        if (client) {
            try {
                stepsIndex = await client.getStepsIndex();
            }
            catch {
                // serve not available — proceed without index
            }
        }
        if (token.isCancellationRequested) {
            return;
        }
        const modelFamily = config.get('model', '');
        this.statusBar.text = '$(loading~spin) OpenBBT AI';
        this.statusBar.show();
        try {
            const completion = await this.callModel(modelFamily, locale, stepsIndex, prefix, token);
            if (!completion || token.isCancellationRequested) {
                return;
            }
            const endOfLine = document.lineAt(position.line).range.end;
            return new vscode.InlineCompletionList([
                new vscode.InlineCompletionItem(completion, new vscode.Range(position, endOfLine))
            ]);
        }
        finally {
            this.statusBar.hide();
        }
    }
    async callModel(modelFamily, locale, stepsIndex, prefix, token) {
        const selector = modelFamily ? { family: modelFamily } : {};
        const models = await vscode.lm.selectChatModels(selector);
        if (models.length === 0) {
            return undefined;
        }
        const prompt = `You are an assistant for writing BDD tests in Gherkin.\n` +
            `Active language: ${locale}\n` +
            `Available steps in this project:\n<steps>\n${stepsIndex}\n</steps>\n` +
            `Suggest the text that completes the current step from the cursor position onward.\n` +
            `Return ONLY the completion text. No explanations, no quotes.\n` +
            `If no step fits, return an empty string.\n\n` +
            `File content up to cursor:\n${prefix}`;
        try {
            const response = await models[0].sendRequest([vscode.LanguageModelChatMessage.User(prompt)], {}, token);
            let text = '';
            for await (const chunk of response.text) {
                if (token.isCancellationRequested) {
                    return undefined;
                }
                text += chunk;
            }
            return text.trim() || undefined;
        }
        catch {
            return undefined;
        }
    }
}
exports.AiCompletionProvider = AiCompletionProvider;
//# sourceMappingURL=aiCompletionProvider.js.map