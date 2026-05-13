"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OpenBBTClient = void 0;
const child_process_1 = require("child_process");
/**
 * Manages an `openbbt serve` subprocess and communicates with it
 * via JSON-RPC 2.0 over stdio with Content-Length framing (same as LSP).
 */
class OpenBBTClient {
    process;
    buffer = Buffer.alloc(0);
    nextId = 1;
    pending = new Map();
    cwd;
    executable;
    log;
    onConnected = undefined;
    constructor(executable, cwd, log = () => { }) {
        this.executable = executable;
        this.cwd = cwd;
        this.log = log;
    }
    get connected() {
        return this.process !== undefined && !this.process.killed;
    }
    connect() {
        if (this.connected) {
            return;
        }
        this.log(`[serve] spawning: ${this.executable} serve (cwd=${this.cwd})`);
        const proc = (0, child_process_1.spawn)(this.executable, ['serve'], {
            cwd: this.cwd,
            stdio: ['pipe', 'pipe', 'pipe'],
        });
        proc.stdout.on('data', (chunk) => this.onData(chunk));
        proc.stderr.on('data', (chunk) => {
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
    async refresh() {
        await this.call('refresh', {});
    }
    async getContributors() {
        return this.call('contributors/list', {});
    }
    async getStepsIndex() {
        const result = await this.call('steps/index', {});
        return JSON.stringify(result);
    }
    async listPlans() {
        return this.call('browse/plans', {});
    }
    async buildPlan() {
        return this.call('browse/plan', {});
    }
    async getNode(nodeId) {
        return this.call('browse/node', { nodeId });
    }
    async getChildren(nodeId) {
        return this.call('browse/children', { nodeId });
    }
    async getPlan(planId) {
        return this.call('plans/get', { planId });
    }
    async listPlansByProject(organization, project, offset = 0, max = 0, withExecutions = false) {
        return this.call('plans/list', { organization, project, offset, max, withExecutions });
    }
    async listExecutionsByPlan(planId, offset = 0, max = 0) {
        return this.call('executions/list', { planId, offset, max });
    }
    async deleteUnexecutedPlans() {
        await this.call('plans/deleteUnexecuted', {});
    }
    async exec(detach = false, suites, profile) {
        const params = { detach };
        if (suites && suites.length > 0) {
            params.suites = suites;
        }
        if (profile) {
            params.profile = profile;
        }
        return this.call('exec', params);
    }
    async rerun(executionId, detach = false) {
        return this.call('exec', { detach, rerun: executionId });
    }
    async getExecutionNode(executionId, planNodeId) {
        try {
            return await this.call('executions/node', { executionId, planNodeId });
        }
        catch {
            return null;
        }
    }
    async listAttachments(executionId, planNodeId) {
        return this.call('executions/attachments', { executionId, planNodeId });
    }
    async getAttachment(executionId, executionNodeId, attachmentId) {
        return this.call('executions/attachment', { executionId, executionNodeId, attachmentId });
    }
    async deleteExecution(executionId) {
        await this.call('executions/delete', { executionId });
    }
    async deletePlan(planId) {
        await this.call('plans/delete', { planId });
    }
    async shutdown() {
        if (!this.connected) {
            return;
        }
        try {
            await this.call('shutdown', {});
        }
        catch {
            // ignore — process may have already exited
        }
        this.process?.kill();
        this.process = undefined;
    }
    // --- Internal ---
    call(method, params) {
        if (!this.connected) {
            this.connect();
        }
        return new Promise((resolve, reject) => {
            const id = this.nextId++;
            this.pending.set(id, { resolve, reject });
            const body = JSON.stringify({ jsonrpc: '2.0', id, method, params });
            const header = `Content-Length: ${Buffer.byteLength(body, 'utf8')}\r\n\r\n`;
            this.log(`[serve →] ${method} (id=${id})`);
            this.process.stdin.write(header + body, 'utf8');
        });
    }
    onData(chunk) {
        this.buffer = Buffer.concat([this.buffer, chunk]);
        while (true) {
            const msg = this.tryReadMessage();
            if (msg === null) {
                break;
            }
            this.handleMessage(msg);
        }
    }
    tryReadMessage() {
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
    handleMessage(body) {
        let msg;
        try {
            msg = JSON.parse(body);
        }
        catch {
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
        }
        else {
            pending.resolve(msg.result);
        }
    }
    rejectAll(err) {
        for (const p of this.pending.values()) {
            p.reject(err);
        }
        this.pending.clear();
    }
}
exports.OpenBBTClient = OpenBBTClient;
//# sourceMappingURL=openbbtClient.js.map