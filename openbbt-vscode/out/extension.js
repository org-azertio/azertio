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
exports.activate = activate;
exports.deactivate = deactivate;
const fs = __importStar(require("fs"));
const path = __importStar(require("path"));
const child_process_1 = require("child_process");
const vscode = __importStar(require("vscode"));
const featureFormatter_1 = require("./featureFormatter");
const gherkinSymbolProvider_1 = require("./gherkinSymbolProvider");
const openbbtClient_1 = require("./openbbtClient");
const executionProvider_1 = require("./executionProvider");
const executionDetailPanel_1 = require("./executionDetailPanel");
const testPlanProvider_1 = require("./testPlanProvider");
const contributorsProvider_1 = require("./contributorsProvider");
const aiCompletionProvider_1 = require("./aiCompletionProvider");
const node_1 = require("vscode-languageclient/node");
let client;
let serveClient;
let extensionContext;
let errorNotificationShowing = false;
const outputChannel = vscode.window.createOutputChannel('OpenBBT');
function logOutput(msg) {
    outputChannel.appendLine(`[${new Date().toISOString()}] ${msg}`);
}
const OPENBBT_YAML_SKELETON = `project:
  organization: My Organization
  name: My Project
  test-suites:
    - name: default
      description: Default test suite
      tag-expression: ""

plugins:
  - gherkin

configuration:
  core:
    resourceFilter: '**/*.feature'

profiles: {}
`;
const EMPTY_YAML_DIAGNOSTIC = 'openbbt.emptyYaml';
const diagnosticCollection = vscode.languages.createDiagnosticCollection('openbbt');
function updateDiagnostics(document) {
    if (!document.fileName.endsWith('openbbt.yaml')) {
        return;
    }
    if (document.getText().trim() === '') {
        const diagnostic = new vscode.Diagnostic(new vscode.Range(0, 0, 0, 0), 'Empty openbbt.yaml. Generate a skeleton to get started.', vscode.DiagnosticSeverity.Hint);
        diagnostic.code = EMPTY_YAML_DIAGNOSTIC;
        diagnosticCollection.set(document.uri, [diagnostic]);
    }
    else {
        diagnosticCollection.delete(document.uri);
    }
}
class OpenbbtYamlCodeLensProvider {
    provideCodeLenses(document) {
        if (document.getText().trim() === '') {
            return [];
        }
        const text = document.getText();
        const lines = text.split('\n');
        const pluginsLineIndex = lines.findIndex(line => /^plugins\s*:/.test(line));
        if (pluginsLineIndex === -1) {
            return [];
        }
        const range = new vscode.Range(pluginsLineIndex, 0, pluginsLineIndex, 0);
        return [
            new vscode.CodeLens(range, {
                title: '$(cloud-download) Install plugins',
                command: 'openbbt.installPlugins',
                tooltip: 'Run openbbt install to download and install plugins',
            }),
        ];
    }
}
class OpenbbtYamlCodeActionProvider {
    static providedCodeActionKinds = [vscode.CodeActionKind.QuickFix];
    provideCodeActions(document, _range, context) {
        const hasDiagnostic = context.diagnostics.some(d => d.code === EMPTY_YAML_DIAGNOSTIC);
        if (!hasDiagnostic) {
            return [];
        }
        const action = new vscode.CodeAction('Generate OpenBBT skeleton', vscode.CodeActionKind.QuickFix);
        action.edit = new vscode.WorkspaceEdit();
        action.edit.insert(document.uri, new vscode.Position(0, 0), OPENBBT_YAML_SKELETON);
        action.isPreferred = true;
        return [action];
    }
}
async function showConnectionError(message) {
    if (errorNotificationShowing) {
        return;
    }
    errorNotificationShowing = true;
    try {
        const openSettings = 'Open Settings';
        const retry = 'Retry';
        const choice = await vscode.window.showErrorMessage(message, openSettings, retry);
        if (choice === openSettings) {
            await vscode.commands.executeCommand('workbench.action.openSettings', 'openbbt.executablePath');
        }
        else if (choice === retry) {
            await startClient();
        }
    }
    finally {
        errorNotificationShowing = false;
    }
}
function executableExists(command) {
    if (path.isAbsolute(command)) {
        return fs.existsSync(command);
    }
    const which = process.platform === 'win32' ? 'where' : 'which';
    return (0, child_process_1.spawnSync)(which, [command]).status === 0;
}
async function startClient() {
    if (!extensionContext) {
        return;
    }
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
        return;
    }
    if (client) {
        try {
            await client.stop();
        }
        catch {
            // client may already be in an error state
        }
        client = undefined;
    }
    const config = vscode.workspace.getConfiguration('openbbt');
    const executable = config.get('executablePath', 'openbbt');
    if (!executableExists(executable)) {
        showConnectionError(`OpenBBT LSP could not connect to '${executable}'. ` +
            `Make sure the CLI is installed or configure the correct path.`);
        return;
    }
    const serverOptions = {
        command: executable,
        args: ['lsp'],
        options: {
            cwd: workspaceFolder.uri.fsPath,
        },
    };
    const clientOptions = {
        documentSelector: [
            { scheme: 'file', language: 'feature' },
            { scheme: 'file', pattern: '**/openbbt.yaml' },
        ],
        workspaceFolder,
        outputChannelName: 'OpenBBT LSP',
        initializationFailedHandler: (_error) => {
            showConnectionError(`OpenBBT LSP could not connect to '${executable}'. ` +
                `Make sure the CLI is installed or configure the correct path.`);
            return false;
        },
        errorHandler: {
            error: () => ({ action: node_1.ErrorAction.Shutdown }),
            closed: () => ({ action: node_1.CloseAction.DoNotRestart }),
        },
    };
    client = new node_1.LanguageClient('openbbt-lsp', 'OpenBBT Language Server', serverOptions, clientOptions);
    await client.start();
    extensionContext.subscriptions.push(client);
}
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
function runPlan(executable, cwd) {
    return new Promise((resolve) => {
        const args = ['plan'];
        logOutput(`[plan] running: ${executable} ${args.join(' ')} (cwd=${cwd})`);
        (0, child_process_1.execFile)(executable, args, { cwd }, (_err, stdout, stderr) => {
            const combined = stdout + '\n' + stderr;
            logOutput(`[plan] stdout: ${stdout.trim() || '(empty)'}`);
            logOutput(`[plan] stderr: ${stderr.trim() || '(empty)'}`);
            if (_err) {
                logOutput(`[plan] exit error: ${_err.message}`);
            }
            let planId;
            for (const line of combined.split('\n')) {
                const trimmed = line.trim();
                if (UUID_PATTERN.test(trimmed)) {
                    planId = trimmed;
                }
            }
            logOutput(`[plan] planId=${planId ?? '(not found)'} hasValidationErrors=${combined.toLowerCase().includes('validation')}`);
            const hasValidationErrors = combined.toLowerCase().includes('validation');
            resolve({ planId, hasValidationErrors });
        });
    });
}
function activate(context) {
    extensionContext = context;
    startClient();
    vscode.workspace.textDocuments.forEach(updateDiagnostics);
    const testPlanProvider = new testPlanProvider_1.TestPlanProvider(logOutput);
    vscode.window.registerTreeDataProvider('openbbt.testPlan', testPlanProvider);
    const contributorsProvider = new contributorsProvider_1.ContributorsProvider(logOutput);
    vscode.window.registerTreeDataProvider('openbbt.contributors', contributorsProvider);
    // Auto-populate the tree on startup using existing plan data (no plan re-run).
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    const executionProvider = new executionProvider_1.ExecutionProvider(workspaceFolder?.uri.fsPath);
    const executionTreeView = vscode.window.createTreeView('openbbt.executions', { treeDataProvider: executionProvider, showCollapseAll: true });
    context.subscriptions.push(executionTreeView, executionProvider);
    if (workspaceFolder) {
        const config = vscode.workspace.getConfiguration('openbbt');
        const executable = config.get('executablePath', 'openbbt');
        if (executableExists(executable)) {
            logOutput('[startup] starting serve connection');
            serveClient = new openbbtClient_1.OpenBBTClient(executable, workspaceFolder.uri.fsPath, logOutput);
            contributorsProvider.setClient(serveClient);
            serveClient.connect();
            testPlanProvider.setClient(serveClient);
            testPlanProvider.invalidate();
            executionProvider.setClient(serveClient);
            executionProvider.refresh();
        }
    }
    context.subscriptions.push(vscode.window.registerFileDecorationProvider({
        provideFileDecoration(uri) {
            if (uri.scheme === testPlanProvider_1.ISSUE_URI_SCHEME) {
                return {
                    color: new vscode.ThemeColor('list.errorForeground'),
                    propagate: false,
                };
            }
        },
    }));
    async function doBuildPlan() {
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        if (!workspaceFolder) {
            vscode.window.showErrorMessage('OpenBBT: no workspace folder open.');
            return;
        }
        const config = vscode.workspace.getConfiguration('openbbt');
        const executable = config.get('executablePath', 'openbbt');
        const cwd = workspaceFolder.uri.fsPath;
        if (serveClient) {
            logOutput(`[build] stopping existing serve process`);
            serveClient.shutdown().catch(() => { });
            serveClient = undefined;
        }
        const planResult = await vscode.window.withProgress({ location: vscode.ProgressLocation.Window, title: 'OpenBBT: building plan…' }, () => runPlan(executable, cwd));
        if (!planResult.planId) {
            vscode.window.showErrorMessage('OpenBBT: plan generation failed. See the OpenBBT output channel for details.');
            outputChannel.show(true);
            return;
        }
        if (planResult.hasValidationErrors) {
            vscode.window.showWarningMessage('OpenBBT: test plan has validation issues.');
        }
        logOutput(`[build] starting new serve connection`);
        serveClient = new openbbtClient_1.OpenBBTClient(executable, cwd, logOutput);
        contributorsProvider.setClient(serveClient);
        serveClient.connect();
        testPlanProvider.setClient(serveClient);
        executionProvider.setClient(serveClient);
        logOutput(`[build] invalidating tree`);
        testPlanProvider.invalidate();
        executionProvider.refresh();
    }
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.testPlan.refresh', async () => {
        await startClient();
        await doBuildPlan();
    }));
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.openSource', async (source) => {
        const match = source.match(/^(.*)\[(\d+),(\d+)\]$/);
        if (!match) {
            return;
        }
        const [, filePath, lineStr, colStr] = match;
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        if (!workspaceFolder) {
            return;
        }
        const fullPath = path.join(workspaceFolder.uri.fsPath, filePath);
        const uri = vscode.Uri.file(fullPath);
        const line = parseInt(lineStr, 10) - 1; // VSCode uses 0-based lines
        const col = parseInt(colStr, 10) - 1; // VSCode uses 0-based columns
        const pos = new vscode.Position(line, col);
        const doc = await vscode.workspace.openTextDocument(uri);
        await vscode.window.showTextDocument(doc, {
            selection: new vscode.Range(pos, pos),
        });
    }));
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.executions.run', async (_item) => {
        if (!serveClient) {
            vscode.window.showErrorMessage('OpenBBT: serve connection not available.');
            return;
        }
        const suitesInput = await vscode.window.showInputBox({
            title: 'OpenBBT: Test Suites',
            prompt: 'Enter test suite names separated by commas, or leave blank to run all suites',
            placeHolder: 'e.g. smoke, regression',
        });
        if (suitesInput === undefined) {
            return;
        }
        const profileInput = await vscode.window.showInputBox({
            title: 'OpenBBT: Profile',
            prompt: 'Enter the profile name to activate, or leave blank for none',
            placeHolder: 'e.g. staging',
        });
        if (profileInput === undefined) {
            return;
        }
        const suites = suitesInput.trim()
            ? suitesInput.split(',').map(s => s.trim()).filter(s => s.length > 0)
            : undefined;
        const profile = profileInput.trim() || undefined;
        try {
            const result = await serveClient.exec(true, suites, profile);
            executionProvider.refresh(true);
            executionProvider.startPolling(result.executionId);
            if (result.planId) {
                const executions = await serveClient.listExecutionsByPlan(result.planId);
                const execItem = executions.find(e => e.executionId === result.executionId);
                if (execItem) {
                    const label = execItem.executedAt.substring(0, 19);
                    await (0, executionDetailPanel_1.openExecutionDetail)(context, serveClient, execItem, label);
                }
            }
            vscode.window.showInformationMessage(`OpenBBT: execution ${result.executionId.substring(0, 8)} started`);
        }
        catch (err) {
            const msg = err instanceof Error ? err.message : String(err);
            vscode.window.showErrorMessage(`OpenBBT: execution failed — ${msg}`);
        }
    }));
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.executions.rerun', async (item) => {
        if (!serveClient) {
            vscode.window.showErrorMessage('OpenBBT: serve connection not available.');
            return;
        }
        const executionId = item?.execution?.executionId;
        if (!executionId) {
            return;
        }
        try {
            const result = await serveClient.rerun(executionId, true);
            executionProvider.refresh(true);
            executionProvider.startPolling(result.executionId);
            if (result.planId) {
                const executions = await serveClient.listExecutionsByPlan(result.planId);
                const execItem = executions.find(e => e.executionId === result.executionId);
                if (execItem) {
                    const label = execItem.executedAt.substring(0, 19);
                    await (0, executionDetailPanel_1.openExecutionDetail)(context, serveClient, execItem, label);
                }
            }
            vscode.window.showInformationMessage(`OpenBBT: execution ${result.executionId.substring(0, 8)} started`);
        }
        catch (err) {
            const msg = err instanceof Error ? err.message : String(err);
            vscode.window.showErrorMessage(`OpenBBT: re-run failed — ${msg}`);
        }
    }));
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.executions.openDetail', async (execution) => {
        if (!serveClient) {
            vscode.window.showErrorMessage('OpenBBT: serve connection not available.');
            return;
        }
        const label = execution.executedAt ? execution.executedAt.substring(0, 19) : execution.executionId.substring(0, 8);
        await (0, executionDetailPanel_1.openExecutionDetail)(context, serveClient, execution, label);
    }));
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.contributors.refresh', async () => {
        await contributorsProvider.refresh();
    }));
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.executions.deleteExecution', async (item) => {
        if (!serveClient) {
            vscode.window.showErrorMessage('OpenBBT: serve connection not available.');
            return;
        }
        const executionId = item?.execution?.executionId;
        if (!executionId) {
            return;
        }
        const label = item?.execution?.executedAt
            ? item.execution.executedAt.substring(0, 19)
            : executionId.substring(0, 8);
        const confirm = await vscode.window.showWarningMessage(`Delete execution ${label}? This cannot be undone.`, { modal: true }, 'Delete');
        if (confirm !== 'Delete') {
            return;
        }
        try {
            await serveClient.deleteExecution(executionId);
            executionProvider.refresh();
        }
        catch (err) {
            const msg = err instanceof Error ? err.message : String(err);
            vscode.window.showErrorMessage(`OpenBBT: failed to delete execution — ${msg}`);
        }
    }));
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.executions.pruneEmpty', async () => {
        if (!serveClient) {
            vscode.window.showErrorMessage('OpenBBT: serve connection not available.');
            return;
        }
        const confirm = await vscode.window.showWarningMessage('Delete all plans with no executions? This cannot be undone.', { modal: true }, 'Delete');
        if (confirm !== 'Delete') {
            return;
        }
        try {
            await serveClient.deleteUnexecutedPlans();
            executionProvider.refresh();
        }
        catch (err) {
            const msg = err instanceof Error ? err.message : String(err);
            vscode.window.showErrorMessage(`OpenBBT: failed to delete unexecuted plans — ${msg}`);
        }
    }));
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.executions.deletePlan', async (item) => {
        if (!serveClient) {
            vscode.window.showErrorMessage('OpenBBT: serve connection not available.');
            return;
        }
        const planId = item?.planId;
        if (!planId) {
            return;
        }
        const confirm = await vscode.window.showWarningMessage(`Delete plan ${planId.substring(0, 8)}… and ALL its executions? This cannot be undone.`, { modal: true }, 'Delete');
        if (confirm !== 'Delete') {
            return;
        }
        try {
            await serveClient.deletePlan(planId);
            executionProvider.refresh();
        }
        catch (err) {
            const msg = err instanceof Error ? err.message : String(err);
            vscode.window.showErrorMessage(`OpenBBT: failed to delete plan — ${msg}`);
        }
    }));
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.showLogs', () => {
        outputChannel.show(true);
    }));
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.installPlugins', async () => {
        const config = vscode.workspace.getConfiguration('openbbt');
        const executable = config.get('executablePath', 'openbbt');
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        const cwd = workspaceFolder?.uri.fsPath;
        const success = await vscode.window.withProgress({ location: vscode.ProgressLocation.Window, title: 'OpenBBT: installing plugins…' }, () => new Promise((resolve) => {
            logOutput(`[install] running: ${executable} install --clean (cwd=${cwd})`);
            (0, child_process_1.execFile)(executable, ['install', '--clean'], { cwd }, (err, stdout, stderr) => {
                logOutput(`[install] stdout: ${stdout.trim() || '(empty)'}`);
                logOutput(`[install] stderr: ${stderr.trim() || '(empty)'}`);
                if (err) {
                    logOutput(`[install] exit error: ${err.message}`);
                }
                resolve(!err);
            });
        }));
        if (!success) {
            vscode.window.showErrorMessage('OpenBBT: plugin installation failed. See the OpenBBT output channel for details.');
            outputChannel.show(true);
            return;
        }
        logOutput('[install] restarting LSP after plugin installation');
        await startClient();
        vscode.window.showInformationMessage('OpenBBT: plugins installed and LSP connection restarted.');
    }));
    context.subscriptions.push(vscode.workspace.onDidChangeConfiguration((event) => {
        if (event.affectsConfiguration('openbbt.executablePath')) {
            startClient();
        }
    }), vscode.languages.registerCodeLensProvider({ scheme: 'file', pattern: '**/openbbt.yaml' }, new OpenbbtYamlCodeLensProvider()), vscode.languages.registerCodeActionsProvider({ scheme: 'file', pattern: '**/openbbt.yaml' }, new OpenbbtYamlCodeActionProvider(), { providedCodeActionKinds: OpenbbtYamlCodeActionProvider.providedCodeActionKinds }), vscode.workspace.onDidOpenTextDocument(updateDiagnostics), vscode.workspace.onDidChangeTextDocument(e => updateDiagnostics(e.document)), vscode.workspace.onDidCloseTextDocument(doc => diagnosticCollection.delete(doc.uri)), vscode.workspace.onDidSaveTextDocument(doc => {
        if (doc.fileName.endsWith('openbbt.yaml')) {
            startClient();
            executionProvider.refresh();
            testPlanProvider.invalidate();
        }
    }), diagnosticCollection, vscode.languages.registerDocumentSymbolProvider({ scheme: 'file', language: 'feature' }, new gherkinSymbolProvider_1.GherkinSymbolProvider()), vscode.languages.registerDocumentFormattingEditProvider({ scheme: 'file', language: 'feature' }, {
        provideDocumentFormattingEdits(document) {
            const formatted = (0, featureFormatter_1.formatFeatureText)(document.getText());
            const full = new vscode.Range(document.positionAt(0), document.positionAt(document.getText().length));
            return [vscode.TextEdit.replace(full, formatted)];
        }
    }));
    const aiStatusBar = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left);
    context.subscriptions.push(aiStatusBar);
    context.subscriptions.push(vscode.languages.registerInlineCompletionItemProvider({ scheme: 'file', language: 'feature' }, new aiCompletionProvider_1.AiCompletionProvider(() => serveClient, aiStatusBar)), vscode.commands.registerCommand('openbbt.ai.toggle', () => {
        const cfg = vscode.workspace.getConfiguration('openbbt.ai');
        const current = cfg.get('enabled', false);
        cfg.update('enabled', !current, vscode.ConfigurationTarget.Global);
        vscode.window.showInformationMessage(`OpenBBT AI completions ${!current ? 'enabled' : 'disabled'}.`);
    }));
}
function deactivate() {
    serveClient?.shutdown();
    serveClient = undefined;
    return client?.stop();
}
//# sourceMappingURL=extension.js.map