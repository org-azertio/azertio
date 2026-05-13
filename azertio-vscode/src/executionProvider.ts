import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';
import { ExecutionListItem, AzertioClient, PlanListItem } from './azertioClient';
import { ISSUE_URI_SCHEME } from './testPlanProvider';

// ---------------------------------------------------------------------------
// azertio.yaml reader
// ---------------------------------------------------------------------------

function readProjectInfo(workspacePath: string): { organization: string; projectName: string } {
    const defaults = { organization: 'Unknown Organization', projectName: 'Unknown Project' };
    try {
        const yamlPath = path.join(workspacePath, 'azertio.yaml');
        const content = fs.readFileSync(yamlPath, 'utf8');
        let inProjectSection = false;
        let organization = defaults.organization;
        let projectName = defaults.projectName;
        for (const raw of content.split('\n')) {
            const line = raw.trimEnd();
            if (/^project\s*:/.test(line)) {
                inProjectSection = true;
                continue;
            }
            if (inProjectSection && /^\S/.test(line) && line.trim() !== '') {
                inProjectSection = false;
            }
            if (inProjectSection) {
                const orgMatch = line.match(/^\s+organization\s*:\s*(.+)/);
                if (orgMatch) { organization = orgMatch[1].trim().replace(/^['"]|['"]$/g, ''); }
                const nameMatch = line.match(/^\s+name\s*:\s*(.+)/);
                if (nameMatch) { projectName = nameMatch[1].trim().replace(/^['"]|['"]$/g, ''); }
            }
        }
        return { organization, projectName };
    } catch {
        return defaults;
    }
}

// ---------------------------------------------------------------------------
// Tree item
// ---------------------------------------------------------------------------

type ExecutionItemKind = 'project' | 'plan' | 'execution';

export class ExecutionItem extends vscode.TreeItem {
    constructor(
        public readonly kind: ExecutionItemKind,
        label: string,
        collapsibleState: vscode.TreeItemCollapsibleState,
        public readonly planId?: string,
        public readonly execution?: ExecutionListItem,
        description?: string,
        hasIssues?: boolean,
    ) {
        super(label, collapsibleState);
        this.id = kind === 'plan' ? planId : kind === 'execution' ? execution?.executionId : 'project';
        this.description = description;
        this.iconPath = resolveIcon(kind, execution?.result);
        this.tooltip = label;
        this.contextValue = kind;
        if (kind === 'plan' && hasIssues) {
            this.resourceUri = vscode.Uri.parse(`${ISSUE_URI_SCHEME}://${planId}`);
        }
        if (kind === 'execution' && execution) {
            this.command = {
                command: 'azertio.executions.openDetail',
                title: 'Open Execution Detail',
                arguments: [execution],
            };
        }
    }
}

function resolveIcon(kind: ExecutionItemKind, result?: string): vscode.ThemeIcon {
    switch (kind) {
        case 'project':   return new vscode.ThemeIcon('organization');
        case 'plan':      return new vscode.ThemeIcon('list-tree');
        case 'execution': return executionIcon(result);
    }
}

function executionIcon(result?: string): vscode.ThemeIcon {
    switch (result) {
        case 'PASSED':  return new vscode.ThemeIcon('pass',           new vscode.ThemeColor('testing.iconPassed'));
        case 'FAILED':  return new vscode.ThemeIcon('error',          new vscode.ThemeColor('testing.iconFailed'));
        case 'ERROR':   return new vscode.ThemeIcon('warning',        new vscode.ThemeColor('testing.iconErrored'));
        case 'SKIPPED': return new vscode.ThemeIcon('debug-step-over',new vscode.ThemeColor('testing.iconSkipped'));
        default:        return new vscode.ThemeIcon('loading~spin');
    }
}

function formatDate(iso: string): string {
    const d = new Date(iso);
    const p = (n: number) => String(n).padStart(2, '0');
    return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

// ---------------------------------------------------------------------------
// Provider
// ---------------------------------------------------------------------------

const POLL_INTERVAL_MS = 1000;
const POLL_MAX_ATTEMPTS = 300; // stop polling after 5 minutes

export class ExecutionProvider implements vscode.TreeDataProvider<ExecutionItem>, vscode.Disposable {

    private readonly _onDidChangeTreeData = new vscode.EventEmitter<ExecutionItem | undefined | void>();
    readonly onDidChangeTreeData = this._onDidChangeTreeData.event;

    private client: AzertioClient | undefined;
    private _expandOnNextLoad = false;

    /** executionIds currently being polled */
    private _pendingExecs = new Set<string>();
    /** executionId → number of poll attempts so far */
    private _pollAttempts = new Map<string, number>();
    private _pollTimer: ReturnType<typeof setInterval> | undefined;

    constructor(private readonly workspacePath: string | undefined) {}

    setClient(client: AzertioClient): void {
        this.client = client;
    }

    refresh(expandPlans = false): void {
        this._expandOnNextLoad = expandPlans;
        this._onDidChangeTreeData.fire();
    }

    startPolling(executionId: string): void {
        this._pendingExecs.add(executionId);
        this._pollAttempts.set(executionId, 0);
        if (!this._pollTimer) {
            this._pollTimer = setInterval(() => this._pollTick(), POLL_INTERVAL_MS);
        }
    }

    dispose(): void {
        this._stopPolling();
    }

    getTreeItem(element: ExecutionItem): vscode.TreeItem {
        return element;
    }

    async getChildren(element?: ExecutionItem): Promise<ExecutionItem[]> {
        if (!element) {
            return [this.projectItem()];
        }
        switch (element.kind) {
            case 'project':   return this.loadPlans();
            case 'plan':      return this.loadExecutions(element.planId!);
            case 'execution': return [];
        }
    }

    // --- Private ---

    private _pollTick(): void {
        if (this._pendingExecs.size === 0) {
            this._stopPolling();
            return;
        }

        // Expire stale polls
        for (const [execId, attempts] of this._pollAttempts) {
            if (attempts + 1 >= POLL_MAX_ATTEMPTS) {
                this._pendingExecs.delete(execId);
                this._pollAttempts.delete(execId);
            } else {
                this._pollAttempts.set(execId, attempts + 1);
            }
        }

        // Refresh tree — loadExecutions will detect completed executions
        this._onDidChangeTreeData.fire();

        if (this._pendingExecs.size === 0) {
            this._stopPolling();
        }
    }

    private _stopPolling(): void {
        if (this._pollTimer) {
            clearInterval(this._pollTimer);
            this._pollTimer = undefined;
        }
        this._pendingExecs.clear();
        this._pollAttempts.clear();
        // Final refresh to show definitive result icons
        this._onDidChangeTreeData.fire();
    }

    private projectItem(): ExecutionItem {
        const { organization, projectName } = this.workspacePath
            ? readProjectInfo(this.workspacePath)
            : { organization: 'Unknown Organization', projectName: 'Unknown Project' };
        return new ExecutionItem(
            'project',
            `${organization} / ${projectName}`,
            vscode.TreeItemCollapsibleState.Expanded,
        );
    }

    private async loadPlans(): Promise<ExecutionItem[]> {
        if (!this.client) {
            return [];
        }
        const { organization, projectName } = this.workspacePath
            ? readProjectInfo(this.workspacePath)
            : { organization: '', projectName: '' };
        try {
            const plans: PlanListItem[] = await this.client.listPlansByProject(organization, projectName, 0, 0, true);
            const expand = this._expandOnNextLoad || this._pendingExecs.size > 0;
            this._expandOnNextLoad = false;
            return plans.map(plan => {
                const parts: string[] = [`${plan.testCaseCount ?? 0}`];
                if (plan.hasIssues) { parts.push('⚠ issues'); }
                parts.push(plan.testCases ? plan.testCases : 'all suites');
                return new ExecutionItem(
                    'plan',
                    formatDate(plan.createdAt),
                    expand ? vscode.TreeItemCollapsibleState.Expanded : vscode.TreeItemCollapsibleState.Collapsed,
                    plan.planId,
                    undefined,
                    parts.join(' | '),
                    plan.hasIssues,
                );
            });
        } catch {
            return [];
        }
    }

    private async loadExecutions(planId: string): Promise<ExecutionItem[]> {
        if (!this.client) {
            return [];
        }
        try {
            const executions: ExecutionListItem[] = await this.client.listExecutionsByPlan(planId);
            // Detect executions that have finished and remove them from the pending set
            if (this._pendingExecs.size > 0) {
                for (const ex of executions) {
                    if (this._pendingExecs.has(ex.executionId) && ex.result !== undefined) {
                        this._pendingExecs.delete(ex.executionId);
                        this._pollAttempts.delete(ex.executionId);
                    }
                }
                if (this._pendingExecs.size === 0) {
                    this._stopPolling();
                }
            }
            return executions.map(ex => {
                const parts: string[] = [];
                if (ex.testPassedCount !== undefined && ex.testErrorCount !== undefined && ex.testFailedCount !== undefined) {
                    const total = ex.testPassedCount + ex.testErrorCount + ex.testFailedCount;
                    parts.push(`${ex.testPassedCount} / ${total}`);
                }
                if (ex.profile) { parts.push(`profile: ${ex.profile}`); }
                return new ExecutionItem(
                    'execution',
                    formatDate(ex.executedAt),
                    vscode.TreeItemCollapsibleState.None,
                    undefined,
                    ex,
                    parts.length > 0 ? parts.join(' | ') : undefined,
                );
            });
        } catch {
            return [];
        }
    }
}