import * as vscode from 'vscode';

const FEATURE_RE  = /^(Feature|Funcionalidad|Caracter[íi]stica)\s*:\s*(.*)/i;
const SCENARIO_RE = /^(Background|Antecedentes|Fondo|Scenario Outline|Scenario Template|Esquema del escenario|Plantilla del escenario|Scenario|Example|Escenario)\s*:\s*(.*)/i;
const EXAMPLES_RE = /^(Examples|Scenarios|Ejemplos)\s*:?(.*)/i;
const STEP_RE     = /^(Given|When|Then|And|But|Dado|Cuando|Entonces|Y|E|\*)\b/i;

function isBackground(keyword: string): boolean {
    return /^(Background|Antecedentes|Fondo)$/i.test(keyword);
}

function isOutline(keyword: string): boolean {
    return /^(Scenario Outline|Scenario Template|Esquema del escenario|Plantilla del escenario)$/i.test(keyword);
}

function lineRange(document: vscode.TextDocument, i: number): vscode.Range {
    return document.lineAt(i).range;
}

function finalize(symbol: vscode.DocumentSymbol, document: vscode.TextDocument, endLine: number): void {
    const end = document.lineAt(Math.min(endLine, document.lineCount - 1)).range.end;
    symbol.range = new vscode.Range(symbol.range.start, end);
}

export class GherkinSymbolProvider implements vscode.DocumentSymbolProvider {

    provideDocumentSymbols(document: vscode.TextDocument): vscode.DocumentSymbol[] {
        const roots: vscode.DocumentSymbol[] = [];
        let feature:  vscode.DocumentSymbol | undefined;
        let scenario: vscode.DocumentSymbol | undefined;
        let examples: vscode.DocumentSymbol | undefined;
        const lastLine = document.lineCount - 1;

        const pushScenario = (endLine: number) => {
            if (!scenario) { return; }
            finalize(scenario, document, endLine);
            (feature ?? { children: roots }).children.push(scenario);
            scenario = undefined;
            examples = undefined;
        };

        for (let i = 0; i <= lastLine; i++) {
            const trimmed = document.lineAt(i).text.trim();
            if (!trimmed || trimmed.startsWith('#')) { continue; }

            let m: RegExpExecArray | null;

            if ((m = FEATURE_RE.exec(trimmed))) {
                pushScenario(i - 1);
                if (feature) { finalize(feature, document, i - 1); }
                const r = lineRange(document, i);
                feature = new vscode.DocumentSymbol(
                    m[1] + ': ' + m[2].trim(),
                    '',
                    vscode.SymbolKind.Module,
                    r, r
                );
                roots.push(feature);
                continue;
            }

            if ((m = SCENARIO_RE.exec(trimmed))) {
                pushScenario(i - 1);
                const r = lineRange(document, i);
                const kind = isBackground(m[1]) ? vscode.SymbolKind.Constructor
                           : isOutline(m[1])    ? vscode.SymbolKind.Interface
                           :                      vscode.SymbolKind.Function;
                const name = m[2].trim() ? m[1] + ': ' + m[2].trim() : m[1];
                scenario = new vscode.DocumentSymbol(name, '', kind, r, r);
                continue;
            }

            if ((m = EXAMPLES_RE.exec(trimmed)) && scenario) {
                if (examples) { finalize(examples, document, i - 1); }
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
        if (feature) { finalize(feature, document, lastLine); }

        return roots;
    }
}