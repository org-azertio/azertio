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
exports.ExecutionProvider = exports.ExecutionItem = void 0;
const fs = __importStar(require("fs"));
const path = __importStar(require("path"));
const vscode = __importStar(require("vscode"));
const testPlanProvider_1 = require("./testPlanProvider");
// ---------------------------------------------------------------------------
// azertio.yaml reader
// ---------------------------------------------------------------------------
function readProjectInfo(workspacePath) {
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
                if (orgMatch) {
                    organization = orgMatch[1].trim().replace(/^['"]|['"]$/g, '');
                }
                const nameMatch = line.match(/^\s+name\s*:\s*(.+)/);
                if (nameMatch) {
                    projectName = nameMatch[1].trim().replace(/^['"]|['"]$/g, '');
                }
            }
        }
        return { organization, projectName };
    }
    catch {
        return defaults;
    }
}
class ExecutionItem extends vscode.TreeItem {
    kind;
    planId;
    execution;
    constructor(kind, label, collapsibleState, planId, execution, description, hasIssues) {
        super(label, collapsibleState);
        this.kind = kind;
        this.planId = planId;
        this.execution = execution;
        this.id = kind === 'plan' ? planId : kind === 'execution' ? execution?.executionId : 'project';
        this.description = description;
        this.iconPath = resolveIcon(kind, execution?.result);
        this.tooltip = label;
        this.contextValue = kind;
        if (kind === 'plan' && hasIssues) {
            this.resourceUri = vscode.Uri.parse(`${testPlanProvider_1.ISSUE_URI_SCHEME}://${planId}`);
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
exports.ExecutionItem = ExecutionItem;
function resolveIcon(kind, result) {
    switch (kind) {
        case 'project': return new vscode.ThemeIcon('organization');
        case 'plan': return new vscode.ThemeIcon('list-tree');
        case 'execution': return executionIcon(result);
    }
}
function executionIcon(result) {
    switch (result) {
        case 'PASSED': return new vscode.ThemeIcon('pass', new vscode.ThemeColor('testing.iconPassed'));
        case 'FAILED': return new vscode.ThemeIcon('error', new vscode.ThemeColor('testing.iconFailed'));
        case 'ERROR': return new vscode.ThemeIcon('warning', new vscode.ThemeColor('testing.iconErrored'));
        case 'SKIPPED': return new vscode.ThemeIcon('debug-step-over', new vscode.ThemeColor('testing.iconSkipped'));
        default: return new vscode.ThemeIcon('loading~spin');
    }
}
function formatDate(iso) {
    const d = new Date(iso);
    const p = (n) => String(n).padStart(2, '0');
    return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}
// ---------------------------------------------------------------------------
// Provider
// ---------------------------------------------------------------------------
const POLL_INTERVAL_MS = 1000;
const POLL_MAX_ATTEMPTS = 300; // stop polling after 5 minutes
class ExecutionProvider {
    workspacePath;
    _onDidChangeTreeData = new vscode.EventEmitter();
    onDidChangeTreeData = this._onDidChangeTreeData.event;
    client;
    _expandOnNextLoad = false;
    /** executionIds currently being polled */
    _pendingExecs = new Set();
    /** executionId → number of poll attempts so far */
    _pollAttempts = new Map();
    _pollTimer;
    constructor(workspacePath) {
        this.workspacePath = workspacePath;
    }
    setClient(client) {
        this.client = client;
    }
    refresh(expandPlans = false) {
        this._expandOnNextLoad = expandPlans;
        this._onDidChangeTreeData.fire();
    }
    startPolling(executionId) {
        this._pendingExecs.add(executionId);
        this._pollAttempts.set(executionId, 0);
        if (!this._pollTimer) {
            this._pollTimer = setInterval(() => this._pollTick(), POLL_INTERVAL_MS);
        }
    }
    dispose() {
        this._stopPolling();
    }
    getTreeItem(element) {
        return element;
    }
    async getChildren(element) {
        if (!element) {
            return [this.projectItem()];
        }
        switch (element.kind) {
            case 'project': return this.loadPlans();
            case 'plan': return this.loadExecutions(element.planId);
            case 'execution': return [];
        }
    }
    // --- Private ---
    _pollTick() {
        if (this._pendingExecs.size === 0) {
            this._stopPolling();
            return;
        }
        // Expire stale polls
        for (const [execId, attempts] of this._pollAttempts) {
            if (attempts + 1 >= POLL_MAX_ATTEMPTS) {
                this._pendingExecs.delete(execId);
                this._pollAttempts.delete(execId);
            }
            else {
                this._pollAttempts.set(execId, attempts + 1);
            }
        }
        // Refresh tree — loadExecutions will detect completed executions
        this._onDidChangeTreeData.fire();
        if (this._pendingExecs.size === 0) {
            this._stopPolling();
        }
    }
    _stopPolling() {
        if (this._pollTimer) {
            clearInterval(this._pollTimer);
            this._pollTimer = undefined;
        }
        this._pendingExecs.clear();
        this._pollAttempts.clear();
        // Final refresh to show definitive result icons
        this._onDidChangeTreeData.fire();
    }
    projectItem() {
        const { organization, projectName } = this.workspacePath
            ? readProjectInfo(this.workspacePath)
            : { organization: 'Unknown Organization', projectName: 'Unknown Project' };
        return new ExecutionItem('project', `${organization} / ${projectName}`, vscode.TreeItemCollapsibleState.Expanded);
    }
    async loadPlans() {
        if (!this.client) {
            return [];
        }
        const { organization, projectName } = this.workspacePath
            ? readProjectInfo(this.workspacePath)
            : { organization: '', projectName: '' };
        try {
            const plans = await this.client.listPlansByProject(organization, projectName, 0, 0, true);
            const expand = this._expandOnNextLoad || this._pendingExecs.size > 0;
            this._expandOnNextLoad = false;
            return plans.map(plan => {
                const parts = [`${plan.testCaseCount ?? 0}`];
                if (plan.hasIssues) {
                    parts.push('⚠ issues');
                }
                parts.push(plan.testCases ? plan.testCases : 'all suites');
                return new ExecutionItem('plan', formatDate(plan.createdAt), expand ? vscode.TreeItemCollapsibleState.Expanded : vscode.TreeItemCollapsibleState.Collapsed, plan.planId, undefined, parts.join(' | '), plan.hasIssues);
            });
        }
        catch {
            return [];
        }
    }
    async loadExecutions(planId) {
        if (!this.client) {
            return [];
        }
        try {
            const executions = await this.client.listExecutionsByPlan(planId);
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
                const parts = [];
                if (ex.testPassedCount !== undefined && ex.testErrorCount !== undefined && ex.testFailedCount !== undefined) {
                    const total = ex.testPassedCount + ex.testErrorCount + ex.testFailedCount;
                    parts.push(`${ex.testPassedCount} / ${total}`);
                }
                if (ex.profile) {
                    parts.push(`profile: ${ex.profile}`);
                }
                return new ExecutionItem('execution', formatDate(ex.executedAt), vscode.TreeItemCollapsibleState.None, undefined, ex, parts.length > 0 ? parts.join(' | ') : undefined);
            });
        }
        catch {
            return [];
        }
    }
}
exports.ExecutionProvider = ExecutionProvider;
//# sourceMappingURL=executionProvider.js.map