import * as vscode from 'vscode';
import { NodeInfo, AzertioClient } from './azertioClient';

type Logger = (msg: string) => void;

export const ISSUE_URI_SCHEME = 'azertio-node-issue';

export class TestPlanItem extends vscode.TreeItem {
    constructor(
        public readonly nodeId: string,
        vsCodeId: string,
        label: string,
        public readonly nodeType: string | null,
        collapsibleState: vscode.TreeItemCollapsibleState,
        hasIssues: boolean,
        source?: string | null,
        description?: string
    ) {
        super(label, collapsibleState);
        this.id = vsCodeId;                // serial-scoped so VSCode forgets expansion on refresh
        this.contextValue = nodeType ?? 'unknown';
        this.iconPath = resolveIcon(nodeType, hasIssues);
        this.tooltip = label;
        if (hasIssues) {
            // resourceUri drives the FileDecorationProvider to color the label
            this.resourceUri = vscode.Uri.parse(`${ISSUE_URI_SCHEME}://${nodeId}`);
        }
        if (source) {
            this.command = {
                command: 'azertio.openSource',
                title: 'Open Source Location',
                arguments: [source],
            };
        }
        if (description) {
            this.description = description;
        }
    }
}

function resolveIcon(nodeType: string | null, hasIssues: boolean): vscode.ThemeIcon {
    const color = hasIssues ? new vscode.ThemeColor('list.errorForeground') : undefined;
    switch (nodeType) {
        case 'TEST_PLAN':    return new vscode.ThemeIcon('list-tree', color);
        case 'TEST_SUITE':   return new vscode.ThemeIcon('folder-library', color);
        case 'TEST_FEATURE': return new vscode.ThemeIcon('file-text', color);
        case 'TEST_CASE':    return new vscode.ThemeIcon('circle-outline', color);
        case 'STEP':         return new vscode.ThemeIcon('symbol-event', color);
        default:             return new vscode.ThemeIcon('circle-outline', color);
    }
}

export class TestPlanProvider implements vscode.TreeDataProvider<TestPlanItem> {

    private readonly _onDidChangeTreeData = new vscode.EventEmitter<TestPlanItem | undefined | void>();
    readonly onDidChangeTreeData = this._onDidChangeTreeData.event;

    private client: AzertioClient | undefined;
    private rootItems: TestPlanItem[] | undefined;
    private loadingPromise: Promise<TestPlanItem[]> | undefined;
    private refreshSerial = 0;
    private readonly log: Logger;

    constructor(log: Logger = () => {}) {
        this.log = log;
    }

    setClient(client: AzertioClient): void {
        this.client = client;
    }

    /**
     * Called after `azertio plan` has run and the serve process is ready.
     * Increments the refresh serial so all tree item IDs change, forcing
     * VSCode to discard its expansion memory and show a clean tree.
     */
    invalidate(): void {
        this.refreshSerial++;
        this.rootItems = undefined;
        this.loadingPromise = undefined;
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: TestPlanItem): vscode.TreeItem {
        return element;
    }

    async getChildren(element?: TestPlanItem): Promise<TestPlanItem[]> {
        if (!this.client) {
            return [];
        }
        if (!element) {
            return this.getRoots();
        }
        try {
            const children = await this.client.getChildren(element.nodeId);
            return children.map(n => this.nodeToItem(n));
        } catch (err) {
            vscode.window.showErrorMessage(`Azertio: failed to load children — ${err}`);
            return [];
        }
    }

    private nodeToItem(node: NodeInfo): TestPlanItem {
        const label = node.display || node.name || node.identifier || node.nodeId;
        const collapsible = node.childCount > 0
            ? vscode.TreeItemCollapsibleState.Collapsed
            : vscode.TreeItemCollapsibleState.None;
        // ID is scoped to the current refresh serial so VSCode treats all items
        // as new after each refresh, giving a clean expansion state.
        const vsCodeId = `${this.refreshSerial}-${node.nodeId}`;
        const description = node.testCaseCount != null ? `${node.testCaseCount}` : undefined;
        return new TestPlanItem(node.nodeId, vsCodeId, label, node.nodeType, collapsible, node.hasIssues, node.source, description);
    }

    private getRoots(): Promise<TestPlanItem[]> {
        if (this.rootItems !== undefined) {
            return Promise.resolve(this.rootItems);
        }
        if (this.loadingPromise) {
            return this.loadingPromise;
        }
        this.loadingPromise = this.fetchRoots();
        return this.loadingPromise;
    }

    private async fetchRoots(): Promise<TestPlanItem[]> {
        try {
            this.log('[tree] calling buildPlan()');
            const plan = await this.client!.buildPlan();
            this.log(`[tree] fetching root node ${plan.planNodeRoot}`);
            const rootNode = await this.client!.getNode(plan.planNodeRoot);
            this.log(`[tree] root node: ${rootNode.nodeType} "${rootNode.name}" childCount=${rootNode.childCount}`);
            this.rootItems = [this.nodeToItem(rootNode)];
            return this.rootItems;
        } catch (err) {
            this.log(`[tree] fetchRoots error: ${err}`);
            vscode.window.showErrorMessage(`Azertio: failed to load test plan — ${err}`);
            this.rootItems = [];
            return [];
        } finally {
            this.loadingPromise = undefined;
        }
    }
}