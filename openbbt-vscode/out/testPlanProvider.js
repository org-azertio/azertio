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
exports.TestPlanProvider = exports.TestPlanItem = exports.ISSUE_URI_SCHEME = void 0;
const vscode = __importStar(require("vscode"));
exports.ISSUE_URI_SCHEME = 'azertio-node-issue';
class TestPlanItem extends vscode.TreeItem {
    nodeId;
    nodeType;
    constructor(nodeId, vsCodeId, label, nodeType, collapsibleState, hasIssues, source, description) {
        super(label, collapsibleState);
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.id = vsCodeId; // serial-scoped so VSCode forgets expansion on refresh
        this.contextValue = nodeType ?? 'unknown';
        this.iconPath = resolveIcon(nodeType, hasIssues);
        this.tooltip = label;
        if (hasIssues) {
            // resourceUri drives the FileDecorationProvider to color the label
            this.resourceUri = vscode.Uri.parse(`${exports.ISSUE_URI_SCHEME}://${nodeId}`);
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
exports.TestPlanItem = TestPlanItem;
function resolveIcon(nodeType, hasIssues) {
    const color = hasIssues ? new vscode.ThemeColor('list.errorForeground') : undefined;
    switch (nodeType) {
        case 'TEST_PLAN': return new vscode.ThemeIcon('list-tree', color);
        case 'TEST_SUITE': return new vscode.ThemeIcon('folder-library', color);
        case 'TEST_FEATURE': return new vscode.ThemeIcon('file-text', color);
        case 'TEST_CASE': return new vscode.ThemeIcon('circle-outline', color);
        case 'STEP': return new vscode.ThemeIcon('symbol-event', color);
        default: return new vscode.ThemeIcon('circle-outline', color);
    }
}
class TestPlanProvider {
    _onDidChangeTreeData = new vscode.EventEmitter();
    onDidChangeTreeData = this._onDidChangeTreeData.event;
    client;
    rootItems;
    loadingPromise;
    refreshSerial = 0;
    log;
    constructor(log = () => { }) {
        this.log = log;
    }
    setClient(client) {
        this.client = client;
    }
    /**
     * Called after `azertio plan` has run and the serve process is ready.
     * Increments the refresh serial so all tree item IDs change, forcing
     * VSCode to discard its expansion memory and show a clean tree.
     */
    invalidate() {
        this.refreshSerial++;
        this.rootItems = undefined;
        this.loadingPromise = undefined;
        this._onDidChangeTreeData.fire();
    }
    getTreeItem(element) {
        return element;
    }
    async getChildren(element) {
        if (!this.client) {
            return [];
        }
        if (!element) {
            return this.getRoots();
        }
        try {
            const children = await this.client.getChildren(element.nodeId);
            return children.map(n => this.nodeToItem(n));
        }
        catch (err) {
            vscode.window.showErrorMessage(`Azertio: failed to load children — ${err}`);
            return [];
        }
    }
    nodeToItem(node) {
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
    getRoots() {
        if (this.rootItems !== undefined) {
            return Promise.resolve(this.rootItems);
        }
        if (this.loadingPromise) {
            return this.loadingPromise;
        }
        this.loadingPromise = this.fetchRoots();
        return this.loadingPromise;
    }
    async fetchRoots() {
        try {
            this.log('[tree] calling buildPlan()');
            const plan = await this.client.buildPlan();
            this.log(`[tree] fetching root node ${plan.planNodeRoot}`);
            const rootNode = await this.client.getNode(plan.planNodeRoot);
            this.log(`[tree] root node: ${rootNode.nodeType} "${rootNode.name}" childCount=${rootNode.childCount}`);
            this.rootItems = [this.nodeToItem(rootNode)];
            return this.rootItems;
        }
        catch (err) {
            this.log(`[tree] fetchRoots error: ${err}`);
            vscode.window.showErrorMessage(`Azertio: failed to load test plan — ${err}`);
            this.rootItems = [];
            return [];
        }
        finally {
            this.loadingPromise = undefined;
        }
    }
}
exports.TestPlanProvider = TestPlanProvider;
//# sourceMappingURL=testPlanProvider.js.map