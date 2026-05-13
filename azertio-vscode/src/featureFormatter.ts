const FEATURE_RE = /^(Feature|Funcionalidad|Característica)\s*:/;
const SCENARIO_RE = /^(Background|Antecedentes|Fondo|Scenario|Scenario Outline|Scenario Template|Example|Escenario|Esquema del escenario|Plantilla del escenario)\s*:/;
const EXAMPLES_RE = /^(Examples|Scenarios|Ejemplos)\s*:/;
const STEP_RE = /^(Given|When|Then|And|But|Dado|Cuando|Entonces|Y|E|\*)\b/;

function parseTableRow(line: string): string[] {
    return line.replace(/^\||\|$/g, '').split('|').map(c => c.trim());
}

function flushTable(buf: string[][], indent: string): string[] {
    if (buf.length === 0) { return []; }
    const widths = buf.reduce((w, row) => {
        row.forEach((c, i) => { w[i] = Math.max(w[i] ?? 0, c.length); });
        return w;
    }, [] as number[]);
    return buf.map(row =>
        indent + '| ' + row.map((c, i) => c.padEnd(widths[i])).join(' | ') + ' |'
    );
}

export function formatFeatureText(text: string): string {
    const lines = text.split(/\r?\n/);
    const out: string[] = [];

    type Block = 'root' | 'feature' | 'scenario';
    let block: Block = 'root';

    let inDocstring = false;
    let docstringDelim = '';
    let docstringTargetIndent = '';
    let docstringOrigIndent = 0;

    let tableBuf: string[][] = [];

    for (const raw of lines) {
        const trimmed = raw.trim();

        // --- inside docstring ---
        if (inDocstring) {
            if (trimmed === docstringDelim) {
                out.push(docstringTargetIndent + docstringDelim);
                inDocstring = false;
            } else {
                // preserve relative indentation of content
                const origIndent = raw.length - raw.trimStart().length;
                const rel = Math.max(0, origIndent - docstringOrigIndent);
                out.push(docstringTargetIndent + ' '.repeat(rel) + raw.trimStart());
            }
            continue;
        }

        // --- table row ---
        if (trimmed.startsWith('|')) {
            tableBuf.push(parseTableRow(trimmed));
            continue;
        }

        // flush buffered table before any other line
        out.push(...flushTable(tableBuf, '      '));
        tableBuf = [];

        // --- docstring open ---
        if (trimmed.startsWith('"""') || trimmed.startsWith('```')) {
            const delim = trimmed.startsWith('"""') ? '"""' : '```';
            const lang = trimmed.slice(delim.length).trim();
            docstringTargetIndent = '      ';
            docstringOrigIndent = raw.length - raw.trimStart().length;
            docstringDelim = delim;
            inDocstring = true;
            out.push(docstringTargetIndent + delim + (lang ? lang : ''));
            continue;
        }

        // --- empty line ---
        if (!trimmed) {
            out.push('');
            continue;
        }

        // --- comment ---
        if (trimmed.startsWith('#')) {
            const indent = block === 'root' ? '' : block === 'feature' ? '  ' : '    ';
            out.push(indent + trimmed);
            continue;
        }

        // --- tag ---
        if (trimmed.startsWith('@')) {
            out.push((block === 'scenario' ? '  ' : '') + trimmed);
            continue;
        }

        // --- Feature ---
        if (FEATURE_RE.test(trimmed)) {
            block = 'feature';
            out.push(trimmed);
            continue;
        }

        // --- Background / Scenario / Scenario Outline ---
        if (SCENARIO_RE.test(trimmed)) {
            block = 'scenario';
            out.push('  ' + trimmed);
            continue;
        }

        // --- Examples ---
        if (EXAMPLES_RE.test(trimmed)) {
            out.push('    ' + trimmed);
            continue;
        }

        // --- Step ---
        if (STEP_RE.test(trimmed)) {
            out.push('    ' + trimmed);
            continue;
        }

        // --- description / free text ---
        const descIndent = block === 'feature' ? '  ' : block === 'scenario' ? '    ' : '';
        out.push(descIndent + trimmed);
    }

    out.push(...flushTable(tableBuf, '      '));

    // single trailing newline
    while (out.length > 0 && out[out.length - 1].trim() === '') {
        out.pop();
    }
    out.push('');

    return out.join('\n');
}