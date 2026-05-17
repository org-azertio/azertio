import * as vscode from 'vscode';
import { AzertioClient, HelpEntry } from './azertioClient';

export const HELP_URI_SCHEME = 'azertio-help';

export class HelpItem extends vscode.TreeItem {
    constructor(public readonly entry: HelpEntry) {
        super(entry.displayName, vscode.TreeItemCollapsibleState.None);
        this.iconPath = new vscode.ThemeIcon('book');
        this.contextValue = 'helpEntry';
        this.command = {
            command: 'azertio.help.open',
            title: 'Open Help',
            arguments: [entry.id],
        };
    }
}

export class HelpProvider implements vscode.TreeDataProvider<HelpItem> {

    private readonly _onDidChangeTreeData = new vscode.EventEmitter<HelpItem | undefined | void>();
    readonly onDidChangeTreeData = this._onDidChangeTreeData.event;

    private client: AzertioClient | undefined;
    private entries: HelpEntry[] = [];

    constructor(private readonly log: (msg: string) => void = () => {}) {}

    setClient(client: AzertioClient): void {
        this.client = client;
    }

    async refresh(): Promise<void> {
        if (!this.client) { return; }
        try {
            this.entries = await this.client.listHelp();
        } catch (err) {
            this.log(`[help] failed to load list: ${err}`);
            this.entries = [];
        }
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: HelpItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: HelpItem): HelpItem[] {
        if (element) { return []; }
        return this.entries.map(e => new HelpItem(e));
    }
}

/**
 * Provides read-only Markdown content fetched via `help/get` for the virtual
 * URI scheme `azertio-help:<id>`. VSCode's built-in Markdown preview renders it.
 */
export class HelpDocumentProvider implements vscode.TextDocumentContentProvider {

    private readonly cache = new Map<string, string>();
    private client: AzertioClient | undefined;

    setClient(client: AzertioClient): void {
        this.client = client;
        this.cache.clear();
    }

    async fetchAndCache(id: string): Promise<void> {
        if (!this.client) { return; }
        try {
            const result = await this.client.getHelp(id);
            this.cache.set(id, result.content);
        } catch {
            this.cache.set(id, `# Error\n\nCould not load help for \`${id}\`.`);
        }
    }

    provideTextDocumentContent(uri: vscode.Uri): string {
        const id = uri.path.startsWith('/') ? uri.path.slice(1) : uri.path;
        return this.cache.get(id) ?? '';
    }

    static uriFor(id: string): vscode.Uri {
        return vscode.Uri.from({ scheme: HELP_URI_SCHEME, path: `/${id}` });
    }
}