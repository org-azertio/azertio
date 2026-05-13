import * as vscode from 'vscode';
import { ContributorTypeInfo, OpenBBTClient, PluginContributors } from './openbbtClient';

export class PluginItem extends vscode.TreeItem {
    constructor(
        public readonly plugin: string,
        public readonly contributors: ContributorTypeInfo[]
    ) {
        super(plugin, vscode.TreeItemCollapsibleState.Collapsed);
        this.iconPath = new vscode.ThemeIcon('package');
        this.description = `${contributors.length} contributor type(s)`;
        this.contextValue = 'contributorPlugin';
    }
}

export class ContributorTypeItem extends vscode.TreeItem {
    constructor(
        public readonly type: string,
        public readonly implementations: string[]
    ) {
        super(type, implementations.length > 0
            ? vscode.TreeItemCollapsibleState.Collapsed
            : vscode.TreeItemCollapsibleState.None);
        this.iconPath = new vscode.ThemeIcon('extensions');
        this.description = `${implementations.length}`;
        this.contextValue = 'contributorType';
    }
}

export class ContributorImplItem extends vscode.TreeItem {
    constructor(name: string) {
        super(name, vscode.TreeItemCollapsibleState.None);
        this.iconPath = new vscode.ThemeIcon('symbol-class');
        this.contextValue = 'contributorImpl';
    }
}

type TreeItem = PluginItem | ContributorTypeItem | ContributorImplItem;

export class ContributorsProvider implements vscode.TreeDataProvider<TreeItem> {

    private readonly _onDidChangeTreeData = new vscode.EventEmitter<TreeItem | undefined | void>();
    readonly onDidChangeTreeData = this._onDidChangeTreeData.event;

    private client: OpenBBTClient | undefined;
    private plugins: PluginContributors[] = [];

    constructor(private readonly log: (msg: string) => void = () => {}) {}

    setClient(client: OpenBBTClient): void {
        this.client = client;
        client.onConnected = () => this.refresh();
    }

    async refresh(): Promise<void> {
        if (!this.client) { return; }
        try {
            this.plugins = await this.client.getContributors();
        } catch (err) {
            this.log(`[contributors] failed to load: ${err}`);
            this.plugins = [];
        }
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: TreeItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: TreeItem): TreeItem[] {
        if (!element) {
            return this.plugins.map(p => new PluginItem(p.plugin, p.contributors));
        }
        if (element instanceof PluginItem) {
            return element.contributors.map(c => new ContributorTypeItem(c.type, c.implementations));
        }
        if (element instanceof ContributorTypeItem) {
            return element.implementations.map(impl => new ContributorImplItem(impl));
        }
        return [];
    }
}