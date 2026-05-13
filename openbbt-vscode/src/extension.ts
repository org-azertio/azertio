import * as fs from 'fs';
import * as path from 'path';
import { execFile, spawnSync } from 'child_process';
import * as vscode from 'vscode';
import { formatFeatureText } from './featureFormatter';
import { GherkinSymbolProvider } from './gherkinSymbolProvider';
import { AzertioClient } from './azertioClient';
import { ExecutionProvider } from './executionProvider';
import { openExecutionDetail } from './executionDetailPanel';
import { ISSUE_URI_SCHEME, TestPlanProvider } from './testPlanProvider';
import { ContributorsProvider } from './contributorsProvider';
import { AiCompletionProvider } from './aiCompletionProvider';
import {
    CloseAction,
    ErrorAction,
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
} from 'vscode-languageclient/node';

let client: LanguageClient | undefined;
let serveClient: AzertioClient | undefined;
let extensionContext: vscode.ExtensionContext | undefined;
let errorNotificationShowing = false;
const outputChannel = vscode.window.createOutputChannel('Azertio');

function logOutput(msg: string): void {
    outputChannel.appendLine(`[${new Date().toISOString()}] ${msg}`);
}

const AZERTIO_YAML_SKELETON = `project:
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

const EMPTY_YAML_DIAGNOSTIC = 'azertio.emptyYaml';
const diagnosticCollection = vscode.languages.createDiagnosticCollection('azertio');

function updateDiagnostics(document: vscode.TextDocument): void {
    if (!document.fileName.endsWith('azertio.yaml')) {
        return;
    }
    if (document.getText().trim() === '') {
        const diagnostic = new vscode.Diagnostic(
            new vscode.Range(0, 0, 0, 0),
            'Empty azertio.yaml. Generate a skeleton to get started.',
            vscode.DiagnosticSeverity.Hint
        );
        diagnostic.code = EMPTY_YAML_DIAGNOSTIC;
        diagnosticCollection.set(document.uri, [diagnostic]);
    } else {
        diagnosticCollection.delete(document.uri);
    }
}

class OpenbbtYamlCodeLensProvider implements vscode.CodeLensProvider {
    provideCodeLenses(document: vscode.TextDocument): vscode.CodeLens[] {
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
                command: 'azertio.installPlugins',
                tooltip: 'Run azertio install to download and install plugins',
            }),
        ];
    }
}

class OpenbbtYamlCodeActionProvider implements vscode.CodeActionProvider {
    static readonly providedCodeActionKinds = [vscode.CodeActionKind.QuickFix];

    provideCodeActions(
        document: vscode.TextDocument,
        _range: vscode.Range,
        context: vscode.CodeActionContext
    ): vscode.CodeAction[] {
        const hasDiagnostic = context.diagnostics.some(d => d.code === EMPTY_YAML_DIAGNOSTIC);
        if (!hasDiagnostic) {
            return [];
        }
        const action = new vscode.CodeAction(
            'Generate Azertio skeleton',
            vscode.CodeActionKind.QuickFix
        );
        action.edit = new vscode.WorkspaceEdit();
        action.edit.insert(document.uri, new vscode.Position(0, 0), AZERTIO_YAML_SKELETON);
        action.isPreferred = true;
        return [action];
    }
}

async function showConnectionError(message: string): Promise<void> {
    if (errorNotificationShowing) {
        return;
    }
    errorNotificationShowing = true;
    try {
        const openSettings = 'Open Settings';
        const retry = 'Retry';
        const choice = await vscode.window.showErrorMessage(message, openSettings, retry);
        if (choice === openSettings) {
            await vscode.commands.executeCommand(
                'workbench.action.openSettings',
                'azertio.executablePath'
            );
        } else if (choice === retry) {
            await startClient();
        }
    } finally {
        errorNotificationShowing = false;
    }
}

function executableExists(command: string): boolean {
    if (path.isAbsolute(command)) {
        return fs.existsSync(command);
    }
    const which = process.platform === 'win32' ? 'where' : 'which';
    return spawnSync(which, [command]).status === 0;
}

async function startClient(): Promise<void> {
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
        } catch {
            // client may already be in an error state
        }
        client = undefined;
    }

    const config = vscode.workspace.getConfiguration('azertio');
    const executable = config.get<string>('executablePath', 'azertio');

    if (!executableExists(executable)) {
        showConnectionError(
            `Azertio LSP could not connect to '${executable}'. ` +
            `Make sure the CLI is installed or configure the correct path.`
        );
        return;
    }

    const serverOptions: ServerOptions = {
        command: executable,
        args: ['lsp'],
        options: {
            cwd: workspaceFolder.uri.fsPath,
        },
    };

    const clientOptions: LanguageClientOptions = {
        documentSelector: [
            { scheme: 'file', language: 'feature' },
            { scheme: 'file', pattern: '**/azertio.yaml' },
        ],
        workspaceFolder,
        outputChannelName: 'Azertio LSP',
        initializationFailedHandler: (_error) => {
            showConnectionError(
                `Azertio LSP could not connect to '${executable}'. ` +
                `Make sure the CLI is installed or configure the correct path.`
            );
            return false;
        },
        errorHandler: {
            error: () => ({ action: ErrorAction.Shutdown }),
            closed: () => ({ action: CloseAction.DoNotRestart }),
        },
    };

    client = new LanguageClient(
        'azertio-lsp',
        'Azertio Language Server',
        serverOptions,
        clientOptions
    );

    await client.start();
    extensionContext.subscriptions.push(client);
}

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

interface PlanResult {
    planId: string | undefined;
    hasValidationErrors: boolean;
}

function runPlan(executable: string, cwd: string): Promise<PlanResult> {
    return new Promise((resolve) => {
        const args = ['plan'];
        logOutput(`[plan] running: ${executable} ${args.join(' ')} (cwd=${cwd})`);
        execFile(executable, args, { cwd }, (_err, stdout, stderr) => {
            const combined = stdout + '\n' + stderr;
            logOutput(`[plan] stdout: ${stdout.trim() || '(empty)'}`);
            logOutput(`[plan] stderr: ${stderr.trim() || '(empty)'}`);
            if (_err) {
                logOutput(`[plan] exit error: ${_err.message}`);
            }
            let planId: string | undefined;
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

export function activate(context: vscode.ExtensionContext): void {
    extensionContext = context;
    startClient();
    vscode.workspace.textDocuments.forEach(updateDiagnostics);

    const testPlanProvider = new TestPlanProvider(logOutput);
    vscode.window.registerTreeDataProvider('azertio.testPlan', testPlanProvider);

    const contributorsProvider = new ContributorsProvider(logOutput);
    vscode.window.registerTreeDataProvider('azertio.contributors', contributorsProvider);

    // Auto-populate the tree on startup using existing plan data (no plan re-run).
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];

    const executionProvider = new ExecutionProvider(workspaceFolder?.uri.fsPath);
    const executionTreeView = vscode.window.createTreeView('azertio.executions', { treeDataProvider: executionProvider, showCollapseAll: true });
    context.subscriptions.push(executionTreeView, executionProvider);
    if (workspaceFolder) {
        const config = vscode.workspace.getConfiguration('azertio');
        const executable = config.get<string>('executablePath', 'azertio');
        if (executableExists(executable)) {
            logOutput('[startup] starting serve connection');
            serveClient = new AzertioClient(executable, workspaceFolder.uri.fsPath, logOutput);
            contributorsProvider.setClient(serveClient);
            serveClient.connect();
            testPlanProvider.setClient(serveClient);
            testPlanProvider.invalidate();
            executionProvider.setClient(serveClient);
            executionProvider.refresh();
        }
    }

    context.subscriptions.push(
        vscode.window.registerFileDecorationProvider({
            provideFileDecoration(uri: vscode.Uri): vscode.FileDecoration | undefined {
                if (uri.scheme === ISSUE_URI_SCHEME) {
                    return {
                        color: new vscode.ThemeColor('list.errorForeground'),
                        propagate: false,
                    };
                }
            },
        })
    );

    async function doBuildPlan(): Promise<void> {
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        if (!workspaceFolder) {
            vscode.window.showErrorMessage('Azertio: no workspace folder open.');
            return;
        }
        const config = vscode.workspace.getConfiguration('azertio');
        const executable = config.get<string>('executablePath', 'azertio');
        const cwd = workspaceFolder.uri.fsPath;

        if (serveClient) {
            logOutput(`[build] stopping existing serve process`);
            serveClient.shutdown().catch(() => {});
            serveClient = undefined;
        }

        const planResult = await vscode.window.withProgress(
            { location: vscode.ProgressLocation.Window, title: 'Azertio: building plan…' },
            () => runPlan(executable, cwd)
        );

        if (!planResult.planId) {
            vscode.window.showErrorMessage(
                'Azertio: plan generation failed. See the Azertio output channel for details.'
            );
            outputChannel.show(true);
            return;
        }
        if (planResult.hasValidationErrors) {
            vscode.window.showWarningMessage('Azertio: test plan has validation issues.');
        }

        logOutput(`[build] starting new serve connection`);
        serveClient = new AzertioClient(executable, cwd, logOutput);
        contributorsProvider.setClient(serveClient);
        serveClient.connect();
        testPlanProvider.setClient(serveClient);
        executionProvider.setClient(serveClient);
        logOutput(`[build] invalidating tree`);
        testPlanProvider.invalidate();
        executionProvider.refresh();
    }

    context.subscriptions.push(
        vscode.commands.registerCommand('azertio.testPlan.refresh', async () => {
            await startClient();
            await doBuildPlan();
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('azertio.openSource', async (source: string) => {
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
            const line = parseInt(lineStr, 10) - 1;   // VSCode uses 0-based lines
            const col  = parseInt(colStr,  10) - 1;   // VSCode uses 0-based columns
            const pos = new vscode.Position(line, col);
            const doc = await vscode.workspace.openTextDocument(uri);
            await vscode.window.showTextDocument(doc, {
                selection: new vscode.Range(pos, pos),
            });
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('azertio.executions.run', async (_item) => {
            if (!serveClient) {
                vscode.window.showErrorMessage('Azertio: serve connection not available.');
                return;
            }

            const suitesInput = await vscode.window.showInputBox({
                title: 'Azertio: Test Suites',
                prompt: 'Enter test suite names separated by commas, or leave blank to run all suites',
                placeHolder: 'e.g. smoke, regression',
            });
            if (suitesInput === undefined) { return; }

            const profileInput = await vscode.window.showInputBox({
                title: 'Azertio: Profile',
                prompt: 'Enter the profile name to activate, or leave blank for none',
                placeHolder: 'e.g. staging',
            });
            if (profileInput === undefined) { return; }

            const suites = suitesInput.trim()
                ? suitesInput.split(',').map(s => s.trim()).filter(s => s.length > 0)
                : undefined;
            const profile = profileInput.trim() || undefined;

            try {
                const result = await serveClient!.exec(true, suites, profile);
                executionProvider.refresh(true);
                executionProvider.startPolling(result.executionId);
                if (result.planId) {
                    const executions = await serveClient!.listExecutionsByPlan(result.planId);
                    const execItem = executions.find(e => e.executionId === result.executionId);
                    if (execItem) {
                        const label = execItem.executedAt.substring(0, 19);
                        await openExecutionDetail(context, serveClient!, execItem, label);
                    }
                }
                vscode.window.showInformationMessage(`Azertio: execution ${result.executionId.substring(0, 8)} started`);
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                vscode.window.showErrorMessage(`Azertio: execution failed — ${msg}`);
            }
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('azertio.executions.rerun', async (item) => {
            if (!serveClient) {
                vscode.window.showErrorMessage('Azertio: serve connection not available.');
                return;
            }
            const executionId: string = item?.execution?.executionId;
            if (!executionId) { return; }
            try {
                const result = await serveClient.rerun(executionId, true);
                executionProvider.refresh(true);
                executionProvider.startPolling(result.executionId);
                if (result.planId) {
                    const executions = await serveClient.listExecutionsByPlan(result.planId);
                    const execItem = executions.find(e => e.executionId === result.executionId);
                    if (execItem) {
                        const label = execItem.executedAt.substring(0, 19);
                        await openExecutionDetail(context, serveClient!, execItem, label);
                    }
                }
                vscode.window.showInformationMessage(`Azertio: execution ${result.executionId.substring(0, 8)} started`);
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                vscode.window.showErrorMessage(`Azertio: re-run failed — ${msg}`);
            }
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('azertio.executions.openDetail', async (execution) => {
            if (!serveClient) {
                vscode.window.showErrorMessage('Azertio: serve connection not available.');
                return;
            }
            const label = execution.executedAt ? execution.executedAt.substring(0, 19) : execution.executionId.substring(0, 8);
            await openExecutionDetail(context, serveClient, execution, label);
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('azertio.contributors.refresh', async () => {
            await contributorsProvider.refresh();
        })
    );


    context.subscriptions.push(
        vscode.commands.registerCommand('azertio.executions.deleteExecution', async (item) => {
            if (!serveClient) {
                vscode.window.showErrorMessage('Azertio: serve connection not available.');
                return;
            }
            const executionId: string = item?.execution?.executionId;
            if (!executionId) { return; }
            const label = item?.execution?.executedAt
                ? item.execution.executedAt.substring(0, 19)
                : executionId.substring(0, 8);
            const confirm = await vscode.window.showWarningMessage(
                `Delete execution ${label}? This cannot be undone.`,
                { modal: true }, 'Delete'
            );
            if (confirm !== 'Delete') { return; }
            try {
                await serveClient.deleteExecution(executionId);
                executionProvider.refresh();
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                vscode.window.showErrorMessage(`Azertio: failed to delete execution — ${msg}`);
            }
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('azertio.executions.pruneEmpty', async () => {
            if (!serveClient) {
                vscode.window.showErrorMessage('Azertio: serve connection not available.');
                return;
            }
            const confirm = await vscode.window.showWarningMessage(
                'Delete all plans with no executions? This cannot be undone.',
                { modal: true }, 'Delete'
            );
            if (confirm !== 'Delete') { return; }
            try {
                await serveClient.deleteUnexecutedPlans();
                executionProvider.refresh();
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                vscode.window.showErrorMessage(`Azertio: failed to delete unexecuted plans — ${msg}`);
            }
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('azertio.executions.deletePlan', async (item) => {
            if (!serveClient) {
                vscode.window.showErrorMessage('Azertio: serve connection not available.');
                return;
            }
            const planId: string = item?.planId;
            if (!planId) { return; }
            const confirm = await vscode.window.showWarningMessage(
                `Delete plan ${planId.substring(0, 8)}… and ALL its executions? This cannot be undone.`,
                { modal: true }, 'Delete'
            );
            if (confirm !== 'Delete') { return; }
            try {
                await serveClient.deletePlan(planId);
                executionProvider.refresh();
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                vscode.window.showErrorMessage(`Azertio: failed to delete plan — ${msg}`);
            }
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('azertio.showLogs', () => {
            outputChannel.show(true);
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('azertio.installPlugins', async () => {
            const config = vscode.workspace.getConfiguration('azertio');
            const executable = config.get<string>('executablePath', 'azertio');
            const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
            const cwd = workspaceFolder?.uri.fsPath;

            const success = await vscode.window.withProgress(
                { location: vscode.ProgressLocation.Window, title: 'Azertio: installing plugins…' },
                () => new Promise<boolean>((resolve) => {
                    logOutput(`[install] running: ${executable} install --clean (cwd=${cwd})`);
                    execFile(executable, ['install', '--clean'], { cwd }, (err, stdout, stderr) => {
                        logOutput(`[install] stdout: ${stdout.trim() || '(empty)'}`);
                        logOutput(`[install] stderr: ${stderr.trim() || '(empty)'}`);
                        if (err) {
                            logOutput(`[install] exit error: ${err.message}`);
                        }
                        resolve(!err);
                    });
                })
            );

            if (!success) {
                vscode.window.showErrorMessage(
                    'Azertio: plugin installation failed. See the Azertio output channel for details.'
                );
                outputChannel.show(true);
                return;
            }

            logOutput('[install] restarting LSP after plugin installation');
            await startClient();
            vscode.window.showInformationMessage('Azertio: plugins installed and LSP connection restarted.');
        })
    );

    context.subscriptions.push(
        vscode.workspace.onDidChangeConfiguration((event) => {
            if (event.affectsConfiguration('azertio.executablePath')) {
                startClient();
            }
        }),
        vscode.languages.registerCodeLensProvider(
            { scheme: 'file', pattern: '**/azertio.yaml' },
            new OpenbbtYamlCodeLensProvider()
        ),
        vscode.languages.registerCodeActionsProvider(
            { scheme: 'file', pattern: '**/azertio.yaml' },
            new OpenbbtYamlCodeActionProvider(),
            { providedCodeActionKinds: OpenbbtYamlCodeActionProvider.providedCodeActionKinds }
        ),
        vscode.workspace.onDidOpenTextDocument(updateDiagnostics),
        vscode.workspace.onDidChangeTextDocument(e => updateDiagnostics(e.document)),
        vscode.workspace.onDidCloseTextDocument(doc => diagnosticCollection.delete(doc.uri)),
        vscode.workspace.onDidSaveTextDocument(doc => {
            if (doc.fileName.endsWith('azertio.yaml')) {
                startClient();
                executionProvider.refresh();
                testPlanProvider.invalidate();
            }
        }),
        diagnosticCollection,
        vscode.languages.registerDocumentSymbolProvider(
            { scheme: 'file', language: 'feature' },
            new GherkinSymbolProvider()
        ),
        vscode.languages.registerDocumentFormattingEditProvider(
            { scheme: 'file', language: 'feature' },
            {
                provideDocumentFormattingEdits(document): vscode.TextEdit[] {
                    const formatted = formatFeatureText(document.getText());
                    const full = new vscode.Range(
                        document.positionAt(0),
                        document.positionAt(document.getText().length)
                    );
                    return [vscode.TextEdit.replace(full, formatted)];
                }
            }
        )
    );

    const aiStatusBar = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left);
    context.subscriptions.push(aiStatusBar);

    context.subscriptions.push(
        vscode.languages.registerInlineCompletionItemProvider(
            { scheme: 'file', language: 'feature' },
            new AiCompletionProvider(() => serveClient, aiStatusBar)
        ),
        vscode.commands.registerCommand('azertio.ai.toggle', () => {
            const cfg = vscode.workspace.getConfiguration('azertio.ai');
            const current = cfg.get<boolean>('enabled', false);
            cfg.update('enabled', !current, vscode.ConfigurationTarget.Global);
            vscode.window.showInformationMessage(
                `Azertio AI completions ${!current ? 'enabled' : 'disabled'}.`
            );
        })
    );
}

export function deactivate(): Thenable<void> | undefined {
    serveClient?.shutdown();
    serveClient = undefined;
    return client?.stop();
}
