import { ChildProcessWithoutNullStreams, spawn } from 'child_process';

export interface PlanInfo {
    planId: string;
    projectId: string;
    createdAt: string;
    planNodeRoot: string;
    testCaseCount: number;
}

export interface PlanListItem {
    planId: string;
    createdAt: string;
    hasIssues: boolean;
    testCaseCount?: number;
    testCases?: string;
}

export interface ExecutionListItem {
    executionId: string;
    planId: string;
    planNodeRoot: string;
    executionRootNodeId: string | null;
    executedAt: string;
    result?: string;
    testPassedCount?: number;
    testErrorCount?: number;
    testFailedCount?: number;
    profile?: string;
}

export interface ExecNodeInfo {
    executionNodeId: string;
    executionId: string;
    planNodeId: string;
    result: string | null;
    startedAt: string | null;
    finishedAt: string | null;
    durationMs: number | null;
    message: string | null;
    attachmentCount: number;
    testPassedCount?: number;
    testErrorCount?: number;
    testFailedCount?: number;
}

export interface AttachmentMeta {
    attachmentId: string;
    executionId: string;
    executionNodeId: string;
    contentType: string;
}

export interface AttachmentData extends AttachmentMeta {
    data: string; // base64
}

export interface ExecResult {
    executionId: string;
    planId: string;
    result?: string;
}

export interface NodeDocument {
    mimeType: string;
    content: string;
}

export interface NodeInfo {
    nodeId: string;
    nodeType: string | null;
    display: string;
    name: string | null;
    identifier: string | null;
    source: string | null;
    keyword: string | null;
    language: string | null;
    validationStatus: string | null;
    validationMessage: string | null;
    hasIssues: boolean;
    tags: string[];
    properties: Record<string, string>;
    childCount: number;
    testCaseCount: number | null;
    document: NodeDocument | null;
    dataTable: string[][] | null;
}

export interface ContributorTypeInfo {
    type: string;
    implementations: string[];
}

export interface PluginContributors {
    plugin: string;
    contributors: ContributorTypeInfo[];
}

type PendingRequest = {
    resolve: (result: unknown) => void;
    reject: (err: Error) => void;
};

/**
 * Manages an `openbbt serve` subprocess and communicates with it
 * via JSON-RPC 2.0 over stdio with Content-Length framing (same as LSP).
 */
export class OpenBBTClient {

    private process: ChildProcessWithoutNullStreams | undefined;
    private buffer = Buffer.alloc(0);
    private nextId = 1;
    private pending = new Map<number, PendingRequest>();
    private readonly cwd: string;
    private readonly executable: string;
    private readonly log: (msg: string) => void;
    onConnected: (() => void) | undefined = undefined;

    constructor(executable: string, cwd: string, log: (msg: string) => void = () => {}) {
        this.executable = executable;
        this.cwd = cwd;
        this.log = log;
    }

    get connected(): boolean {
        return this.process !== undefined && !this.process.killed;
    }

    connect(): void {
        if (this.connected) {
            return;
        }
        this.log(`[serve] spawning: ${this.executable} serve (cwd=${this.cwd})`);
        const proc = spawn(this.executable, ['serve'], {
            cwd: this.cwd,
            stdio: ['pipe', 'pipe', 'pipe'],
        });
        proc.stdout.on('data', (chunk: Buffer) => this.onData(chunk));
        proc.stderr.on('data', (chunk: Buffer) => {
            this.log(`[serve stderr] ${chunk.toString('utf8').trimEnd()}`);
        });
        proc.on('error', (err) => {
            this.log(`[serve error] ${err.message}`);
            this.process = undefined;
            this.rejectAll(err);
        });
        proc.on('close', (code) => {
            this.log(`[serve] process closed (exit code ${code})`);
            this.process = undefined;
            this.rejectAll(new Error(`openbbt serve process exited (code ${code})`));
        });
        this.process = proc;
        this.onConnected?.();
    }

    async refresh(): Promise<void> {
        await this.call('refresh', {});
    }

    async getContributors(): Promise<PluginContributors[]> {
        return this.call('contributors/list', {}) as Promise<PluginContributors[]>;
    }

    async getStepsIndex(): Promise<string> {
        const result = await this.call('steps/index', {});
        return JSON.stringify(result);
    }

    async listPlans(): Promise<PlanInfo[]> {
        return this.call('browse/plans', {}) as Promise<PlanInfo[]>;
    }

    async buildPlan(): Promise<PlanInfo> {
        return this.call('browse/plan', {}) as Promise<PlanInfo>;
    }

    async getNode(nodeId: string): Promise<NodeInfo> {
        return this.call('browse/node', { nodeId }) as Promise<NodeInfo>;
    }

    async getChildren(nodeId: string): Promise<NodeInfo[]> {
        return this.call('browse/children', { nodeId }) as Promise<NodeInfo[]>;
    }

    async getPlan(planId: string): Promise<{ planId: string; createdAt: string; planNodeRoot: string; organization?: string; project?: string; description?: string; suites?: string }> {
        return this.call('plans/get', { planId }) as Promise<{ planId: string; createdAt: string; planNodeRoot: string; organization?: string; project?: string; description?: string; suites?: string }>;
    }

    async listPlansByProject(organization: string, project: string, offset = 0, max = 0, withExecutions = false): Promise<PlanListItem[]> {
        return this.call('plans/list', { organization, project, offset, max, withExecutions }) as Promise<PlanListItem[]>;
    }

    async listExecutionsByPlan(planId: string, offset = 0, max = 0): Promise<ExecutionListItem[]> {
        return this.call('executions/list', { planId, offset, max }) as Promise<ExecutionListItem[]>;
    }

    async deleteUnexecutedPlans(): Promise<void> {
        await this.call('plans/deleteUnexecuted', {});
    }

    async exec(detach = false, suites?: string[], profile?: string): Promise<ExecResult> {
        const params: Record<string, unknown> = { detach };
        if (suites && suites.length > 0) { params.suites = suites; }
        if (profile) { params.profile = profile; }
        return this.call('exec', params) as Promise<ExecResult>;
    }

    async rerun(executionId: string, detach = false): Promise<ExecResult> {
        return this.call('exec', { detach, rerun: executionId }) as Promise<ExecResult>;
    }

    async getExecutionNode(executionId: string, planNodeId: string): Promise<ExecNodeInfo | null> {
        try {
            return await this.call('executions/node', { executionId, planNodeId }) as ExecNodeInfo;
        } catch {
            return null;
        }
    }

    async listAttachments(executionId: string, planNodeId: string): Promise<AttachmentMeta[]> {
        return this.call('executions/attachments', { executionId, planNodeId }) as Promise<AttachmentMeta[]>;
    }

    async getAttachment(executionId: string, executionNodeId: string, attachmentId: string): Promise<AttachmentData> {
        return this.call('executions/attachment', { executionId, executionNodeId, attachmentId }) as Promise<AttachmentData>;
    }

    async deleteExecution(executionId: string): Promise<void> {
        await this.call('executions/delete', { executionId });
    }

    async deletePlan(planId: string): Promise<void> {
        await this.call('plans/delete', { planId });
    }

    async shutdown(): Promise<void> {
        if (!this.connected) {
            return;
        }
        try {
            await this.call('shutdown', {});
        } catch {
            // ignore — process may have already exited
        }
        this.process?.kill();
        this.process = undefined;
    }

    // --- Internal ---

    private call(method: string, params: object): Promise<unknown> {
        if (!this.connected) {
            this.connect();
        }
        return new Promise((resolve, reject) => {
            const id = this.nextId++;
            this.pending.set(id, { resolve, reject });
            const body = JSON.stringify({ jsonrpc: '2.0', id, method, params });
            const header = `Content-Length: ${Buffer.byteLength(body, 'utf8')}\r\n\r\n`;
            this.log(`[serve →] ${method} (id=${id})`);
            this.process!.stdin.write(header + body, 'utf8');
        });
    }

    private onData(chunk: Buffer): void {
        this.buffer = Buffer.concat([this.buffer, chunk]);
        while (true) {
            const msg = this.tryReadMessage();
            if (msg === null) {
                break;
            }
            this.handleMessage(msg);
        }
    }

    private tryReadMessage(): string | null {
        const headerEnd = this.buffer.indexOf('\r\n\r\n');
        if (headerEnd === -1) {
            return null;
        }
        const headerStr = this.buffer.subarray(0, headerEnd).toString('utf8');
        let contentLength = -1;
        for (const line of headerStr.split('\r\n')) {
            if (line.toLowerCase().startsWith('content-length:')) {
                contentLength = parseInt(line.substring(15).trim(), 10);
            }
        }
        if (contentLength < 0) {
            return null;
        }
        const bodyStart = headerEnd + 4;
        if (this.buffer.length < bodyStart + contentLength) {
            return null;
        }
        const body = this.buffer.subarray(bodyStart, bodyStart + contentLength).toString('utf8');
        this.buffer = this.buffer.subarray(bodyStart + contentLength);
        return body;
    }

    private handleMessage(body: string): void {
        let msg: { id?: number; result?: unknown; error?: { code: number; message: string } };
        try {
            msg = JSON.parse(body);
        } catch {
            this.log(`[serve ←] unparseable response: ${body}`);
            return;
        }
        if (msg.id === undefined) {
            return; // notification — ignore
        }
        this.log(`[serve ←] response id=${msg.id} ${msg.error ? 'ERROR: ' + msg.error.message : 'OK'}`);
        const pending = this.pending.get(msg.id);
        if (!pending) {
            return;
        }
        this.pending.delete(msg.id);
        if (msg.error) {
            pending.reject(new Error(`JSON-RPC error ${msg.error.code}: ${msg.error.message}`));
        } else {
            pending.resolve(msg.result);
        }
    }

    private rejectAll(err: Error): void {
        for (const p of this.pending.values()) {
            p.reject(err);
        }
        this.pending.clear();
    }
}
