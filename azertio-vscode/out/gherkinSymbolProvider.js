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
exports.GherkinSymbolProvider = void 0;
const vscode = __importStar(require("vscode"));
const FEATURE_RE = /^(Feature|Funcionalidad|Caracter[íi]stica)\s*:\s*(.*)/i;
const SCENARIO_RE = /^(Background|Antecedentes|Fondo|Scenario Outline|Scenario Template|Esquema del escenario|Plantilla del escenario|Scenario|Example|Escenario)\s*:\s*(.*)/i;
const EXAMPLES_RE = /^(Examples|Scenarios|Ejemplos)\s*:?(.*)/i;
const STEP_RE = /^(Given|When|Then|And|But|Dado|Cuando|Entonces|Y|E|\*)\b/i;
function isBackground(keyword) {
    return /^(Background|Antecedentes|Fondo)$/i.test(keyword);
}
function isOutline(keyword) {
    return /^(Scenario Outline|Scenario Template|Esquema del escenario|Plantilla del escenario)$/i.test(keyword);
}
function lineRange(document, i) {
    return document.lineAt(i).range;
}
function finalize(symbol, document, endLine) {
    const end = document.lineAt(Math.min(endLine, document.lineCount - 1)).range.end;
    symbol.range = new vscode.Range(symbol.range.start, end);
}
class GherkinSymbolProvider {
    provideDocumentSymbols(document) {
        const roots = [];
        let feature;
        let scenario;
        let examples;
        const lastLine = document.lineCount - 1;
        const pushScenario = (endLine) => {
            if (!scenario) {
                return;
            }
            finalize(scenario, document, endLine);
            (feature ?? { children: roots }).children.push(scenario);
            scenario = undefined;
            examples = undefined;
        };
        for (let i = 0; i <= lastLine; i++) {
            const trimmed = document.lineAt(i).text.trim();
            if (!trimmed || trimmed.startsWith('#')) {
                continue;
            }
            let m;
            if ((m = FEATURE_RE.exec(trimmed))) {
                pushScenario(i - 1);
                if (feature) {
                    finalize(feature, document, i - 1);
                }
                const r = lineRange(document, i);
                feature = new vscode.DocumentSymbol(m[1] + ': ' + m[2].trim(), '', vscode.SymbolKind.Module, r, r);
                roots.push(feature);
                continue;
            }
            if ((m = SCENARIO_RE.exec(trimmed))) {
                pushScenario(i - 1);
                const r = lineRange(document, i);
                const kind = isBackground(m[1]) ? vscode.SymbolKind.Constructor
                    : isOutline(m[1]) ? vscode.SymbolKind.Interface
                        : vscode.SymbolKind.Function;
                const name = m[2].trim() ? m[1] + ': ' + m[2].trim() : m[1];
                scenario = new vscode.DocumentSymbol(name, '', kind, r, r);
                continue;
            }
            if ((m = EXAMPLES_RE.exec(trimmed)) && scenario) {
                if (examples) {
                    finalize(examples, document, i - 1);
                }
                const r = lineRange(document, i);
                const name = m[2].trim() ? m[1] + ': ' + m[2].trim() : m[1];
                examples = new vscode.DocumentSymbol(name, '', vscode.SymbolKind.Array, r, r);
                scenario.children.push(examples);
                continue;
            }
            if (STEP_RE.test(trimmed) && scenario) {
                const r = lineRange(document, i);
                const step = new vscode.DocumentSymbol(trimmed, '', vscode.SymbolKind.Event, r, r);
                (examples ?? scenario).children.push(step);
                continue;
            }
        }
        pushScenario(lastLine);
        if (feature) {
            finalize(feature, document, lastLine);
        }
        return roots;
    }
}
exports.GherkinSymbolProvider = GherkinSymbolProvider;
//# sourceMappingURL=gherkinSymbolProvider.js.map