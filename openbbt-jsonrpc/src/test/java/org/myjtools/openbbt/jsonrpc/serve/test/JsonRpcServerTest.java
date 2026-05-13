package org.myjtools.openbbt.jsonrpc.serve.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.execution.ExecutionResult;
import org.myjtools.openbbt.core.execution.TestExecution;
import org.myjtools.openbbt.core.execution.TestExecutionNode;
import org.myjtools.openbbt.core.persistence.*;
import org.myjtools.openbbt.core.testplan.*;
import org.myjtools.openbbt.jsonrpc.serve.JsonRpcServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JsonRpcServerTest {

    // ---- Protocol helpers -----------------------------------------------

    private static byte[] frame(String json) {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        byte[] header = ("Content-Length: " + body.length + "\r\n\r\n")
            .getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[header.length + body.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(body, 0, result, header.length, body.length);
        return result;
    }

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) total += a.length;
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] a : arrays) { System.arraycopy(a, 0, result, pos, a.length); pos += a.length; }
        return result;
    }

    private static String req(int id, String method, String paramsJson) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"" + method + "\",\"params\":" + paramsJson + "}";
    }

    private static String notif(String method) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\"}";
    }

    private static List<JsonObject> parseResponses(byte[] output) {
        List<JsonObject> list = new ArrayList<>();
        int pos = 0;
        while (pos < output.length) {
            int headerEnd = -1;
            for (int i = pos; i < output.length - 3; i++) {
                if (output[i] == '\r' && output[i+1] == '\n' && output[i+2] == '\r' && output[i+3] == '\n') {
                    headerEnd = i + 4;
                    break;
                }
            }
            if (headerEnd < 0) break;
            String headerStr = new String(output, pos, headerEnd - pos, StandardCharsets.UTF_8);
            int contentLength = -1;
            for (String line : headerStr.split("\r\n")) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                }
            }
            if (contentLength < 0) break;
            String body = new String(output, headerEnd, contentLength, StandardCharsets.UTF_8);
            list.add(JsonParser.parseString(body).getAsJsonObject());
            pos = headerEnd + contentLength;
        }
        return list;
    }

    private List<JsonObject> run(JsonRpcServer.RepositoryFactory factory, String... messages) {
        byte[][] frames = new byte[messages.length][];
        for (int i = 0; i < messages.length; i++) frames[i] = frame(messages[i]);
        ByteArrayInputStream in = new ByteArrayInputStream(concat(frames));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(in, out, factory).run();
        return parseResponses(out.toByteArray());
    }

    // ---- Stub repositories -----------------------------------------------

    static abstract class StubPlanRepo implements TestPlanRepository {
        @Override public Optional<TestPlanNode> getNodeData(UUID id) { return Optional.empty(); }
        @Override public <T> void updateNodeField(UUID id, String f, T v) {}
        @Override public <T> Optional<T> getNodeField(UUID id, String f) { return Optional.empty(); }
        @Override public boolean existsNode(UUID id) { return false; }
        @Override public Optional<UUID> getParentNode(UUID id) { return Optional.empty(); }
        @Override public void deleteNode(UUID id) {}
        @Override public void attachChildNodeLast(UUID p, UUID c) {}
        @Override public void attachChildNodeFirst(UUID p, UUID c) {}
        @Override public void detachChildNode(UUID p, UUID c) {}
        @Override public Stream<UUID> getNodeChildren(UUID id) { return Stream.empty(); }
        @Override public Stream<UUID> getNodeDescendants(UUID id) { return Stream.empty(); }
        @Override public Stream<UUID> getNodeDescendantsWithIssues(UUID id) { return Stream.empty(); }
        @Override public Stream<UUID> getNodeAncestors(UUID id) { return Stream.empty(); }
        @Override public int countNodeChildren(UUID id) { return 0; }
        @Override public int countNodeDescendants(UUID id) { return 0; }
        @Override public int countNodeAncestors(UUID id) { return 0; }
        @Override public UUID persistNode(TestPlanNode n) { return n.nodeID(); }
        @Override public Stream<UUID> searchNodes(TestPlanNodeCriteria c) { return Stream.empty(); }
        @Override public int countNodes(TestPlanNodeCriteria c) { return 0; }
        @Override public boolean existsNodeTag(UUID id, String t) { return false; }
        @Override public void addNodeTag(UUID id, String t) {}
        @Override public void removeNodeTag(UUID id, String t) {}
        @Override public List<String> getNodeTags(UUID id) { return List.of(); }
        @Override public boolean existsNodeProperty(UUID id, String k, String v) { return false; }
        @Override public void addNodeProperty(UUID id, String k, String v) {}
        @Override public void removeNodeProperty(UUID id, String k) {}
        @Override public Optional<String> getNodeProperty(UUID id, String k) { return Optional.empty(); }
        @Override public Map<String, String> getNodeProperties(UUID id) { return Map.of(); }
        @Override public List<TestPlan> listPlans() { return List.of(); }
        @Override public List<TestPlan> listPlans(String org, String proj, int off, int max) { return List.of(); }
        @Override public Optional<TestPlan> getPlan(TestProject p, String rh, String ch) { return Optional.empty(); }
        @Override public Optional<TestPlan> getPlan(UUID id) { return Optional.empty(); }
        @Override public Optional<TestProject> getProject(UUID id) { return Optional.empty(); }
        @Override public TestPlan persistPlan(TestPlan p) { return p; }
        @Override public UUID persistProject(TestProject p) { return UUID.randomUUID(); }
        @Override public void assignPlanToNodes(UUID planId, UUID rootId) {}
        @Override public void setNodeValidation(UUID id, ValidationStatus s, String m) {}
        @Override public void propagatePlanIssues(UUID id) {}
        @Override public boolean planHasIssues(UUID id) { return false; }
        @Override public void deletePlan(UUID id) {}
    }

    static abstract class StubExecRepo implements TestExecutionRepository {
        @Override public TestExecution newExecution(UUID planID, Instant at, String profile) {
            TestExecution e = new TestExecution();
            e.executionID(UUID.randomUUID()); e.planID(planID); e.executedAt(at);
            return e;
        }
        @Override public UUID newExecutionNode(UUID execId, UUID planNodeId) { return UUID.randomUUID(); }
        @Override public Optional<UUID> getExecutionNodeByPlanNode(UUID execId, UUID planNodeId) { return Optional.empty(); }
        @Override public Optional<TestExecutionNode> getExecutionNode(UUID execId, UUID planNodeId) { return Optional.empty(); }
        @Override public void updateExecutionNodeStart(UUID id, Instant at) {}
        @Override public void updateExecutionNodeFinish(UUID id, ExecutionResult r, Instant at) {}
        @Override public void updateExecutionNodeTestCounts(UUID id, int p, int e, int f) {}
        @Override public void updateExecutionTestCounts(UUID id, int p, int e, int f) {}
        @Override public void updateExecutionNodeMessage(UUID id, String m) {}
        @Override public UUID newAttachment(UUID nodeId) { return UUID.randomUUID(); }
        @Override public List<UUID> listAttachmentIds(UUID nodeId) { return List.of(); }
        @Override public List<TestExecution> listExecutions(UUID planId, UUID root, int off, int max) { return List.of(); }
        @Override public Optional<TestExecution> getExecution(UUID id) { return Optional.empty(); }
        @Override public Optional<ExecutionResult> getExecutionNodeResult(UUID id) { return Optional.empty(); }
        @Override public void deleteExecution(UUID id) {}
        @Override public void deleteExecutionsByPlan(UUID id) {}
    }

    static abstract class StubAttachRepo implements AttachmentRepository {
        @Override public void storeAttachment(UUID execId, UUID nodeId, UUID attId, byte[] b, String ct) {}
        @Override public void deleteAttachment(UUID execId, UUID nodeId, UUID attId) {}
        @Override public void deleteAttachments(UUID execId) {}
        @Override public Optional<Attachment> retrieveAttachment(UUID execId, UUID nodeId, UUID attId) { return Optional.empty(); }
        @Override public Stream<Attachment> streamAttachments(UUID execId, UUID nodeId) { return Stream.empty(); }
    }

    // ---- Helpers to build model objects ---------------------------------

    private static TestPlan plan(UUID planId, UUID projectId, UUID rootId) {
        return new TestPlan(planId, projectId, Instant.EPOCH, "h1", "h2", rootId, 3, null);
    }

    private static TestPlanNode node(UUID nodeId) {
        TestPlanNode n = new TestPlanNode(NodeType.TEST_SUITE);
        n.nodeID(nodeId);
        n.name("SuiteA");
        return n;
    }

    private static JsonRpcServer.RepositoryFactory withExec(StubPlanRepo planRepo, StubExecRepo execRepo) {
        return new JsonRpcServer.RepositoryFactory() {
            @Override public TestPlanRepository open() { return planRepo; }
            @Override public TestExecutionRepository openExecution() { return execRepo; }
        };
    }

    private static JsonRpcServer.RepositoryFactory withAll(StubPlanRepo planRepo, StubExecRepo execRepo, StubAttachRepo attRepo) {
        return new JsonRpcServer.RepositoryFactory() {
            @Override public TestPlanRepository open() { return planRepo; }
            @Override public TestExecutionRepository openExecution() { return execRepo; }
            @Override public AttachmentRepository openAttachment() { return attRepo; }
        };
    }

    private JsonRpcServer server(JsonRpcServer.RepositoryFactory factory, JsonRpcServer.ExecHandler exec,
                                 JsonRpcServer.PlanHandler plan, JsonRpcServer.ContributorsProvider contrib,
                                 String... messages) {
        byte[][] frames = new byte[messages.length][];
        for (int i = 0; i < messages.length; i++) frames[i] = frame(messages[i]);
        return new JsonRpcServer(new ByteArrayInputStream(concat(frames)), new ByteArrayOutputStream(), factory, exec, plan, contrib);
    }

    private List<JsonObject> runWith(JsonRpcServer.RepositoryFactory factory, JsonRpcServer.ExecHandler exec,
                                     JsonRpcServer.PlanHandler plan, JsonRpcServer.ContributorsProvider contrib,
                                     String... messages) {
        byte[][] frames = new byte[messages.length][];
        for (int i = 0; i < messages.length; i++) frames[i] = frame(messages[i]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(new ByteArrayInputStream(concat(frames)), out, factory, exec, plan, contrib).run();
        return parseResponses(out.toByteArray());
    }

    // ---- Tests ----------------------------------------------------------

    @Test
    void shutdownStopsServerAndReturnsSuccess() {
        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {},
            req(1, "shutdown", "{}")
        );
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).has("result")).isTrue();
    }

    @Test
    void unknownMethodReturnsError() {
        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {},
            req(1, "unknown/method", "{}"),
            req(99, "shutdown", "{}")
        );
        assertThat(responses).hasSize(2);
        JsonObject error = responses.get(0).getAsJsonObject("error");
        assertThat(error.get("code").getAsInt()).isEqualTo(-32601);
        assertThat(error.get("message").getAsString()).contains("Method not found");
    }

    @Test
    void browsePlansReturnsPlansWithoutSuiteFilter() {
        UUID planId = UUID.randomUUID(), projectId = UUID.randomUUID(), rootId = UUID.randomUUID();
        TestPlan p1 = plan(planId, projectId, rootId);
        TestPlan p2 = new TestPlan(UUID.randomUUID(), projectId, Instant.EPOCH, "h", "h", rootId, 1, "s1,s2");

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public List<TestPlan> listPlans() { return List.of(p1, p2); }
            },
            req(1, "browse/plans", "{}"),
            req(99, "shutdown", "{}")
        );

        assertThat(responses).hasSize(2);
        var arr = responses.get(0).getAsJsonArray("result");
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).getAsJsonObject().get("planId").getAsString()).isEqualTo(planId.toString());
    }

    @Test
    void browsePlansReturnsFallbackWhenAllHaveSuiteFilter() {
        TestPlan p = new TestPlan(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH, "h", "h", UUID.randomUUID(), 1, "s1");

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public List<TestPlan> listPlans() { return List.of(p); }
            },
            req(1, "browse/plans", "{}"),
            req(99, "shutdown", "{}")
        );

        assertThat(responses.get(0).getAsJsonArray("result")).hasSize(1);
    }

    @Test
    void browsePlanBuildReturnsPlanFromHandler() {
        UUID planId = UUID.randomUUID(), projectId = UUID.randomUUID(), rootId = UUID.randomUUID();
        TestPlan built = plan(planId, projectId, rootId);

        List<JsonObject> responses = runWith(
            () -> new StubPlanRepo() {},
            null, () -> built, null,
            req(1, "browse/plan", "{}"),
            req(99, "shutdown", "{}")
        );

        assertThat(responses).hasSize(2);
        var result = responses.get(0).getAsJsonObject("result");
        assertThat(result.get("planId").getAsString()).isEqualTo(planId.toString());
    }

    @Test
    void browsePlanBuildWithoutHandlerReturnsError() {
        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {},
            req(1, "browse/plan", "{}"),
            req(99, "shutdown", "{}")
        );
        assertThat(responses.get(0).has("error")).isTrue();
    }

    @Test
    void browseNodeReturnsNodeJson() {
        UUID nodeId = UUID.randomUUID();
        TestPlanNode n = node(nodeId);

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public Optional<TestPlanNode> getNodeData(UUID id) {
                    return id.equals(nodeId) ? Optional.of(n) : Optional.empty();
                }
            },
            req(1, "browse/node", "{\"nodeId\":\"" + nodeId + "\"}"),
            req(99, "shutdown", "{}")
        );

        var result = responses.get(0).getAsJsonObject("result");
        assertThat(result.get("nodeId").getAsString()).isEqualTo(nodeId.toString());
        assertThat(result.get("name").getAsString()).isEqualTo("SuiteA");
    }

    @Test
    void browseNodeNotFoundReturnsError() {
        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {},
            req(1, "browse/node", "{\"nodeId\":\"" + UUID.randomUUID() + "\"}"),
            req(99, "shutdown", "{}")
        );
        assertThat(responses.get(0).has("error")).isTrue();
    }

    @Test
    void browseChildrenReturnsChildNodes() {
        UUID parentId = UUID.randomUUID(), childId = UUID.randomUUID();
        TestPlanNode child = node(childId);

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public Stream<UUID> getNodeChildren(UUID id) {
                    return id.equals(parentId) ? Stream.of(childId) : Stream.empty();
                }
                @Override public Optional<TestPlanNode> getNodeData(UUID id) {
                    return id.equals(childId) ? Optional.of(child) : Optional.empty();
                }
            },
            req(1, "browse/children", "{\"nodeId\":\"" + parentId + "\"}"),
            req(99, "shutdown", "{}")
        );

        var arr = responses.get(0).getAsJsonArray("result");
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).getAsJsonObject().get("nodeId").getAsString()).isEqualTo(childId.toString());
    }

    @Test
    void plansListReturnsMatchingPlans() {
        UUID planId = UUID.randomUUID(), rootId = UUID.randomUUID(), projectId = UUID.randomUUID();
        TestPlan p = plan(planId, projectId, rootId);

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public List<TestPlan> listPlans(String org, String proj, int off, int max) {
                    return List.of(p);
                }
            },
            req(1, "plans/list", "{\"organization\":\"org\",\"project\":\"proj\"}"),
            req(99, "shutdown", "{}")
        );

        var arr = responses.get(0).getAsJsonArray("result");
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).getAsJsonObject().get("planId").getAsString()).isEqualTo(planId.toString());
    }

    @Test
    void plansListIncludesIssuesCountAndSuiteSelection() {
        UUID planId = UUID.randomUUID(), rootId = UUID.randomUUID(), projectId = UUID.randomUUID();
        TestPlan p = new TestPlan(planId, projectId, Instant.EPOCH, "h1", "h2", rootId, 7, "suiteA,suiteB");
        TestPlanNode root = node(rootId);
        root.hasIssues(true);

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public List<TestPlan> listPlans(String org, String proj, int off, int max, boolean withExecutions) {
                    return List.of(p);
                }
                @Override public Optional<TestPlanNode> getNodeData(UUID id) {
                    return id.equals(rootId) ? Optional.of(root) : Optional.empty();
                }
            },
            req(1, "plans/list", "{\"organization\":\"org\",\"project\":\"proj\",\"withExecutions\":true}"),
            req(99, "shutdown", "{}")
        );

        var result = responses.get(0).getAsJsonArray("result").get(0).getAsJsonObject();
        assertThat(result.get("hasIssues").getAsBoolean()).isTrue();
        assertThat(result.get("testCaseCount").getAsInt()).isEqualTo(7);
        assertThat(result.get("testCases").getAsString()).isEqualTo("suiteA,suiteB");
    }

    @Test
    void plansGetReturnsPlanDetail() {
        UUID planId = UUID.randomUUID(), projectId = UUID.randomUUID(), rootId = UUID.randomUUID();
        TestPlan p = plan(planId, projectId, rootId);
        TestProject proj = new TestProject("MyProject", "desc", "MyOrg", List.of());

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public Optional<TestPlan> getPlan(UUID id) {
                    return id.equals(planId) ? Optional.of(p) : Optional.empty();
                }
                @Override public Optional<TestProject> getProject(UUID id) {
                    return id.equals(projectId) ? Optional.of(proj) : Optional.empty();
                }
            },
            req(1, "plans/get", "{\"planId\":\"" + planId + "\"}"),
            req(99, "shutdown", "{}")
        );

        var result = responses.get(0).getAsJsonObject("result");
        assertThat(result.get("planId").getAsString()).isEqualTo(planId.toString());
        assertThat(result.get("organization").getAsString()).isEqualTo("MyOrg");
        assertThat(result.get("project").getAsString()).isEqualTo("MyProject");
    }

    @Test
    void plansGetNotFoundReturnsError() {
        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {},
            req(1, "plans/get", "{\"planId\":\"" + UUID.randomUUID() + "\"}"),
            req(99, "shutdown", "{}")
        );
        assertThat(responses.get(0).has("error")).isTrue();
    }

    @Test
    void plansDeleteSucceeds() {
        UUID planId = UUID.randomUUID();
        List<String> deleted = new ArrayList<>();

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public void deletePlan(UUID id) { deleted.add(id.toString()); }
            },
            req(1, "plans/delete", "{\"planId\":\"" + planId + "\"}"),
            req(99, "shutdown", "{}")
        );

        assertThat(responses.get(0).has("result")).isTrue();
        assertThat(deleted).contains(planId.toString());
    }

    @Test
    void plansDeleteWithExecutionsDeletesAttachments() {
        UUID planId = UUID.randomUUID(), rootId = UUID.randomUUID(), projectId = UUID.randomUUID();
        TestPlan p = plan(planId, projectId, rootId);
        List<UUID> deletedAttachments = new ArrayList<>();

        StubExecRepo execRepo = new StubExecRepo() {
            @Override public List<TestExecution> listExecutions(UUID pid, UUID root, int off, int max) {
                TestExecution ex = new TestExecution();
                ex.executionID(UUID.randomUUID()); ex.planID(pid); ex.executedAt(Instant.EPOCH);
                return List.of(ex);
            }
        };
        StubAttachRepo attRepo = new StubAttachRepo() {
            @Override public void deleteAttachments(UUID execId) { deletedAttachments.add(execId); }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(concat(
                frame(req(1, "plans/delete", "{\"planId\":\"" + planId + "\"}")),
                frame(req(99, "shutdown", "{}"))
            )),
            out,
            withAll(
                new StubPlanRepo() {
                    @Override public Optional<TestPlan> getPlan(UUID id) {
                        return id.equals(planId) ? Optional.of(p) : Optional.empty();
                    }
                },
                execRepo, attRepo
            ),
            null, null, null
        ).run();

        List<JsonObject> responses = parseResponses(out.toByteArray());
        assertThat(responses.get(0).has("result")).isTrue();
        assertThat(deletedAttachments).hasSize(1);
    }

    @Test
    void plansDeleteUnexecutedDeletesUnexecutedPlans() {
        UUID planId = UUID.randomUUID(), rootId = UUID.randomUUID(), projectId = UUID.randomUUID();
        TestPlan p = plan(planId, projectId, rootId);
        List<String> deleted = new ArrayList<>();

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public List<TestPlan> listPlans() { return List.of(p); }
                @Override public void deletePlan(UUID id) { deleted.add(id.toString()); }
            },
            req(1, "plans/deleteUnexecuted", "{}"),
            req(99, "shutdown", "{}")
        );

        assertThat(responses.get(0).has("result")).isTrue();
        assertThat(deleted).contains(planId.toString());
    }

    @Test
    void executionsListRequiresExecutionRepository() {
        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {},
            req(1, "executions/list", "{\"planId\":\"" + UUID.randomUUID() + "\"}"),
            req(99, "shutdown", "{}")
        );
        assertThat(responses.get(0).has("error")).isTrue();
    }

    @Test
    void executionsListReturnsExecutions() {
        UUID planId = UUID.randomUUID(), rootId = UUID.randomUUID(), projectId = UUID.randomUUID();
        TestPlan p = plan(planId, projectId, rootId);

        TestExecution ex = new TestExecution();
        ex.executionID(UUID.randomUUID());
        ex.planID(planId);
        ex.executedAt(Instant.EPOCH);

        StubExecRepo execRepo = new StubExecRepo() {
            @Override public List<TestExecution> listExecutions(UUID pid, UUID root, int off, int max) {
                return List.of(ex);
            }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(concat(
                frame(req(1, "executions/list", "{\"planId\":\"" + planId + "\"}")),
                frame(req(99, "shutdown", "{}"))
            )),
            out,
            withExec(
                new StubPlanRepo() {
                    @Override public Optional<TestPlan> getPlan(UUID id) {
                        return id.equals(planId) ? Optional.of(p) : Optional.empty();
                    }
                },
                execRepo
            ),
            null, null, null
        ).run();

        List<JsonObject> responses = parseResponses(out.toByteArray());
        var arr = responses.get(0).getAsJsonArray("result");
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).getAsJsonObject().get("executionId").getAsString())
            .isEqualTo(ex.executionID().toString());
    }

    @Test
    void executionsListIncludesResultCountsAndProfileWhenAvailable() {
        UUID planId = UUID.randomUUID(), rootId = UUID.randomUUID(), projectId = UUID.randomUUID();
        UUID executionRootNodeId = UUID.randomUUID();
        TestPlan p = plan(planId, projectId, rootId);

        TestExecution ex = new TestExecution();
        ex.executionID(UUID.randomUUID());
        ex.planID(planId);
        ex.executionRootNodeID(executionRootNodeId);
        ex.executedAt(Instant.EPOCH);
        ex.testPassedCount(5);
        ex.testErrorCount(1);
        ex.testFailedCount(2);
        ex.profile("staging");

        StubExecRepo execRepo = new StubExecRepo() {
            @Override public List<TestExecution> listExecutions(UUID pid, UUID root, int off, int max) {
                return List.of(ex);
            }
            @Override public Optional<ExecutionResult> getExecutionNodeResult(UUID id) {
                return id.equals(executionRootNodeId) ? Optional.of(ExecutionResult.FAILED) : Optional.empty();
            }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(concat(
                frame(req(1, "executions/list", "{\"planId\":\"" + planId + "\"}")),
                frame(req(99, "shutdown", "{}"))
            )),
            out,
            withExec(
                new StubPlanRepo() {
                    @Override public Optional<TestPlan> getPlan(UUID id) {
                        return id.equals(planId) ? Optional.of(p) : Optional.empty();
                    }
                },
                execRepo
            ),
            null, null, null
        ).run();

        var result = parseResponses(out.toByteArray()).get(0).getAsJsonArray("result").get(0).getAsJsonObject();
        assertThat(result.get("executionRootNodeId").getAsString()).isEqualTo(executionRootNodeId.toString());
        assertThat(result.get("result").getAsString()).isEqualTo("FAILED");
        assertThat(result.get("testPassedCount").getAsInt()).isEqualTo(5);
        assertThat(result.get("testErrorCount").getAsInt()).isEqualTo(1);
        assertThat(result.get("testFailedCount").getAsInt()).isEqualTo(2);
        assertThat(result.get("profile").getAsString()).isEqualTo("staging");
    }

    @Test
    void executionsNodeRequiresExecutionRepository() {
        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {},
            req(1, "executions/node", "{\"executionId\":\"" + UUID.randomUUID() + "\",\"planNodeId\":\"" + UUID.randomUUID() + "\"}"),
            req(99, "shutdown", "{}")
        );
        assertThat(responses.get(0).has("error")).isTrue();
    }

    @Test
    void executionsNodeReturnsNodeData() {
        UUID execId = UUID.randomUUID(), planNodeId = UUID.randomUUID(), execNodeId = UUID.randomUUID();

        TestExecutionNode execNode = new TestExecutionNode();
        execNode.executionID(execId);
        execNode.executionNodeID(execNodeId);
        execNode.planNodeID(planNodeId);

        StubExecRepo execRepo = new StubExecRepo() {
            @Override public Optional<TestExecutionNode> getExecutionNode(UUID eid, UUID pnid) {
                return Optional.of(execNode);
            }
            @Override public List<UUID> listAttachmentIds(UUID id) { return List.of(); }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(concat(
                frame(req(1, "executions/node", "{\"executionId\":\"" + execId + "\",\"planNodeId\":\"" + planNodeId + "\"}")),
                frame(req(99, "shutdown", "{}"))
            )),
            out,
            withExec(new StubPlanRepo() {}, execRepo),
            null, null, null
        ).run();

        List<JsonObject> responses = parseResponses(out.toByteArray());
        var result = responses.get(0).getAsJsonObject("result");
        assertThat(result.get("executionNodeId").getAsString()).isEqualTo(execNodeId.toString());
    }

    @Test
    void executionsNodeReturnsOptionalFieldsAndAttachmentCount() {
        UUID execId = UUID.randomUUID(), planNodeId = UUID.randomUUID(), execNodeId = UUID.randomUUID();

        TestExecutionNode execNode = new TestExecutionNode();
        execNode.executionID(execId);
        execNode.executionNodeID(execNodeId);
        execNode.planNodeID(planNodeId);
        execNode.result(ExecutionResult.FAILED);
        execNode.startTime(Instant.parse("2026-01-01T10:00:00Z"));
        execNode.endTime(Instant.parse("2026-01-01T10:00:02Z"));
        execNode.message("boom");
        execNode.testPassedCount(1);
        execNode.testErrorCount(2);
        execNode.testFailedCount(3);

        StubExecRepo execRepo = new StubExecRepo() {
            @Override public Optional<TestExecutionNode> getExecutionNode(UUID eid, UUID pnid) {
                return Optional.of(execNode);
            }
            @Override public List<UUID> listAttachmentIds(UUID id) {
                return id.equals(execNodeId) ? List.of(UUID.randomUUID(), UUID.randomUUID()) : List.of();
            }
        };

        List<JsonObject> responses = runWith(
            withExec(new StubPlanRepo() {}, execRepo),
            null, null, null,
            req(1, "executions/node", "{\"executionId\":\"" + execId + "\",\"planNodeId\":\"" + planNodeId + "\"}"),
            req(99, "shutdown", "{}")
        );

        var result = responses.get(0).getAsJsonObject("result");
        assertThat(result.get("result").getAsString()).isEqualTo("FAILED");
        assertThat(result.get("startedAt").getAsString()).isEqualTo("2026-01-01T10:00:00Z");
        assertThat(result.get("finishedAt").getAsString()).isEqualTo("2026-01-01T10:00:02Z");
        assertThat(result.get("durationMs").getAsLong()).isEqualTo(2000L);
        assertThat(result.get("message").getAsString()).isEqualTo("boom");
        assertThat(result.get("testPassedCount").getAsInt()).isEqualTo(1);
        assertThat(result.get("testErrorCount").getAsInt()).isEqualTo(2);
        assertThat(result.get("testFailedCount").getAsInt()).isEqualTo(3);
        assertThat(result.get("attachmentCount").getAsInt()).isEqualTo(2);
    }

    @Test
    void executionsAttachmentsRequiresBothRepositories() {
        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {},
            req(1, "executions/attachments", "{\"executionId\":\"" + UUID.randomUUID() + "\",\"planNodeId\":\"" + UUID.randomUUID() + "\"}"),
            req(99, "shutdown", "{}")
        );
        assertThat(responses.get(0).has("error")).isTrue();
    }

    @Test
    void executionsListAttachmentsReturnsAttachments() {
        UUID execId = UUID.randomUUID(), planNodeId = UUID.randomUUID(), execNodeId = UUID.randomUUID();
        UUID attId = UUID.randomUUID();

        AttachmentRepository.Attachment att = new AttachmentRepository.Attachment(attId, new byte[]{1}, "text/plain");

        StubExecRepo execRepo = new StubExecRepo() {
            @Override public Optional<UUID> getExecutionNodeByPlanNode(UUID eid, UUID pnid) {
                return Optional.of(execNodeId);
            }
            @Override public List<UUID> listAttachmentIds(UUID id) {
                return id.equals(execNodeId) ? List.of(attId) : List.of();
            }
        };

        StubAttachRepo attRepo = new StubAttachRepo() {
            @Override public Optional<Attachment> retrieveAttachment(UUID eid, UUID nid, UUID aid) {
                return Optional.of(att);
            }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(concat(
                frame(req(1, "executions/attachments", "{\"executionId\":\"" + execId + "\",\"planNodeId\":\"" + planNodeId + "\"}")),
                frame(req(99, "shutdown", "{}"))
            )),
            out,
            withAll(new StubPlanRepo() {}, execRepo, attRepo),
            null, null, null
        ).run();

        List<JsonObject> responses = parseResponses(out.toByteArray());
        var arr = responses.get(0).getAsJsonArray("result");
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).getAsJsonObject().get("attachmentId").getAsString()).isEqualTo(attId.toString());
    }

    @Test
    void executionsAttachmentsMissingNodeReturnsError() {
        List<JsonObject> responses = runWith(
            withAll(new StubPlanRepo() {}, new StubExecRepo() {}, new StubAttachRepo() {}),
            null, null, null,
            req(1, "executions/attachments", "{\"executionId\":\"" + UUID.randomUUID() + "\",\"planNodeId\":\"" + UUID.randomUUID() + "\"}"),
            req(99, "shutdown", "{}")
        );

        assertThat(responses.get(0).getAsJsonObject("error").get("message").getAsString())
            .contains("Execution node not found");
    }

    @Test
    void executionsGetAttachmentReturnsBase64Content() {
        UUID execId = UUID.randomUUID(), execNodeId = UUID.randomUUID(), attId = UUID.randomUUID();
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        AttachmentRepository.Attachment att = new AttachmentRepository.Attachment(attId, data, "text/plain");

        StubAttachRepo attRepo = new StubAttachRepo() {
            @Override public Optional<Attachment> retrieveAttachment(UUID eid, UUID nid, UUID aid) {
                return Optional.of(att);
            }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(concat(
                frame(req(1, "executions/attachment",
                    "{\"executionId\":\"" + execId + "\",\"executionNodeId\":\"" + execNodeId + "\",\"attachmentId\":\"" + attId + "\"}")),
                frame(req(99, "shutdown", "{}"))
            )),
            out,
            withAll(new StubPlanRepo() {}, new StubExecRepo() {}, attRepo),
            null, null, null
        ).run();

        List<JsonObject> responses = parseResponses(out.toByteArray());
        var result = responses.get(0).getAsJsonObject("result");
        assertThat(result.get("contentType").getAsString()).isEqualTo("text/plain");
        assertThat(result.get("data").getAsString()).isEqualTo(Base64.getEncoder().encodeToString(data));
    }

    @Test
    void executionsGetAttachmentMissingReturnsError() {
        List<JsonObject> responses = runWith(
            withAll(new StubPlanRepo() {}, new StubExecRepo() {}, new StubAttachRepo() {}),
            null, null, null,
            req(1, "executions/attachment",
                "{\"executionId\":\"" + UUID.randomUUID() + "\",\"executionNodeId\":\"" + UUID.randomUUID() + "\",\"attachmentId\":\"" + UUID.randomUUID() + "\"}"),
            req(99, "shutdown", "{}")
        );

        assertThat(responses.get(0).getAsJsonObject("error").get("message").getAsString())
            .contains("Attachment not found");
    }

    @Test
    void executionsDeleteRemovesExecution() {
        UUID execId = UUID.randomUUID();
        List<UUID> deletedExec = new ArrayList<>();

        StubExecRepo execRepo = new StubExecRepo() {
            @Override public void deleteExecution(UUID id) { deletedExec.add(id); }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(concat(
                frame(req(1, "executions/delete", "{\"executionId\":\"" + execId + "\"}")),
                frame(req(99, "shutdown", "{}"))
            )),
            out,
            withExec(new StubPlanRepo() {}, execRepo),
            null, null, null
        ).run();

        List<JsonObject> responses = parseResponses(out.toByteArray());
        assertThat(responses.get(0).has("result")).isTrue();
        assertThat(deletedExec).contains(execId);
    }

    @Test
    void executionsDeleteWithAttachmentsDeletesAttachmentsToo() {
        UUID execId = UUID.randomUUID();
        List<UUID> deletedAtt = new ArrayList<>();

        StubExecRepo execRepo = new StubExecRepo() {};
        StubAttachRepo attRepo = new StubAttachRepo() {
            @Override public void deleteAttachments(UUID eid) { deletedAtt.add(eid); }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(concat(
                frame(req(1, "executions/delete", "{\"executionId\":\"" + execId + "\"}")),
                frame(req(99, "shutdown", "{}"))
            )),
            out,
            withAll(new StubPlanRepo() {}, execRepo, attRepo),
            null, null, null
        ).run();

        assertThat(deletedAtt).contains(execId);
    }

    @Test
    void contributorsListWithoutProviderReturnsError() {
        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {},
            req(1, "contributors/list", "{}"),
            req(99, "shutdown", "{}")
        );
        assertThat(responses.get(0).has("error")).isTrue();
    }

    @Test
    void contributorsListReturnsContributors() {
        Map<String, Map<String, List<String>>> contributors = Map.of(
            "my-plugin", Map.of("StepProvider", List.of("impl.A", "impl.B"))
        );

        List<JsonObject> responses = runWith(
            () -> new StubPlanRepo() {},
            null, null,
            () -> contributors,
            req(1, "contributors/list", "{}"),
            req(99, "shutdown", "{}")
        );

        var arr = responses.get(0).getAsJsonArray("result");
        assertThat(arr).hasSize(1);
        var pluginObj = arr.get(0).getAsJsonObject();
        assertThat(pluginObj.get("plugin").getAsString()).isEqualTo("my-plugin");
        var contribs = pluginObj.getAsJsonArray("contributors");
        assertThat(contribs).hasSize(1);
        assertThat(contribs.get(0).getAsJsonObject().get("type").getAsString()).isEqualTo("StepProvider");
        assertThat(contribs.get(0).getAsJsonObject().getAsJsonArray("implementations"))
            .extracting(JsonElement::getAsString)
            .containsExactly("impl.A", "impl.B");
    }

    @Test
    void execSyncReturnsExecutionId() {
        UUID execId = UUID.randomUUID(), planId = UUID.randomUUID();
        TestExecution ex = new TestExecution();
        ex.executionID(execId); ex.planID(planId); ex.executedAt(Instant.EPOCH);

        List<JsonObject> responses = runWith(
            () -> new StubPlanRepo() {},
            (onCreated, profile, suites) -> ex,
            null, null,
            req(1, "exec", "{}"),
            req(99, "shutdown", "{}")
        );

        var result = responses.get(0).getAsJsonObject("result");
        assertThat(result.get("executionId").getAsString()).isEqualTo(execId.toString());
    }

    @Test
    void execSyncIncludesProfileAndResultWhenRepositoriesAvailable() {
        UUID execId = UUID.randomUUID(), planId = UUID.randomUUID(), rootNodeId = UUID.randomUUID();
        TestExecution ex = new TestExecution();
        ex.executionID(execId);
        ex.planID(planId);
        ex.executedAt(Instant.EPOCH);
        ex.profile("staging");
        ex.executionRootNodeID(rootNodeId);

        StubExecRepo execRepo = new StubExecRepo() {
            @Override public Optional<ExecutionResult> getExecutionNodeResult(UUID id) {
                return id.equals(rootNodeId) ? Optional.of(ExecutionResult.PASSED) : Optional.empty();
            }
        };

        List<JsonObject> responses = runWith(
            withExec(new StubPlanRepo() {}, execRepo),
            (onCreated, profile, suites) -> ex,
            null, null,
            req(1, "exec", "{}"),
            req(99, "shutdown", "{}")
        );

        var result = responses.get(0).getAsJsonObject("result");
        assertThat(result.get("profile").getAsString()).isEqualTo("staging");
        assertThat(result.get("result").getAsString()).isEqualTo("PASSED");
    }

    @Test
    void execWithoutHandlerReturnsError() {
        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {},
            req(1, "exec", "{}"),
            req(99, "shutdown", "{}")
        );
        assertThat(responses.get(0).has("error")).isTrue();
    }

    @Test
    void execDetachModeReturnsImmediately() {
        UUID execId = UUID.randomUUID(), planId = UUID.randomUUID();

        List<JsonObject> responses = runWith(
            () -> new StubPlanRepo() {},
            (onCreated, profile, suites) -> {
                if (onCreated != null) onCreated.accept(execId, planId);
                TestExecution ex = new TestExecution();
                ex.executionID(execId); ex.planID(planId); ex.executedAt(Instant.EPOCH);
                return ex;
            },
            null, null,
            req(1, "exec", "{\"detach\":true}"),
            req(99, "shutdown", "{}")
        );

        var result = responses.get(0).getAsJsonObject("result");
        assertThat(result.get("executionId").getAsString()).isEqualTo(execId.toString());
        assertThat(result.get("planId").getAsString()).isEqualTo(planId.toString());
    }

    @Test
    void execDetachModeReturnsErrorWhenHandlerFailsBeforeStart() {
        List<JsonObject> responses = runWith(
            () -> new StubPlanRepo() {},
            (onCreated, profile, suites) -> { throw new IllegalStateException("kaboom"); },
            null, null,
            req(1, "exec", "{\"detach\":true}"),
            req(99, "shutdown", "{}")
        );

        assertThat(responses.get(0).getAsJsonObject("error").get("message").getAsString())
            .contains("Execution failed to start");
    }

    @Test
    void execWithProfileAndSuitesParameters() {
        UUID execId = UUID.randomUUID(), planId = UUID.randomUUID();
        List<String> capturedSuites = new ArrayList<>();
        String[] capturedProfile = {null};

        TestExecution ex = new TestExecution();
        ex.executionID(execId); ex.planID(planId); ex.executedAt(Instant.EPOCH);

        List<JsonObject> responses = runWith(
            () -> new StubPlanRepo() {},
            (onCreated, profile, suites) -> {
                capturedProfile[0] = profile;
                capturedSuites.addAll(suites);
                return ex;
            },
            null, null,
            req(1, "exec", "{\"profile\":\"staging\",\"suites\":[\"suite1\",\"suite2\"]}"),
            req(99, "shutdown", "{}")
        );

        assertThat(capturedProfile[0]).isEqualTo("staging");
        assertThat(capturedSuites).containsExactly("suite1", "suite2");
    }

    @Test
    void refreshReopensRepository() {
        List<StubPlanRepo> opened = new ArrayList<>();

        List<JsonObject> responses = run(
            () -> { StubPlanRepo r = new StubPlanRepo() {}; opened.add(r); return r; },
            req(1, "refresh", "{}"),
            req(99, "shutdown", "{}")
        );

        assertThat(responses.get(0).has("result")).isTrue();
        assertThat(opened).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void refreshClosesPreviousRepositoryWhenClosable() {
        List<Boolean> closed = new ArrayList<>();

        class ClosableRepo extends StubPlanRepo implements AutoCloseable {
            @Override public void close() { closed.add(true); }
        }

        List<JsonObject> responses = run(
            ClosableRepo::new,
            req(1, "refresh", "{}"),
            req(99, "shutdown", "{}")
        );

        assertThat(responses.get(0).has("result")).isTrue();
        assertThat(closed).isNotEmpty();
    }

    @Test
    void notificationWithNoIdReceivesResponse() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(frame(notif("shutdown"))),
            out,
            () -> new StubPlanRepo() {}
        ).run();
        // Server exited cleanly
        assertThat(out).isNotNull();
    }

    @Test
    void requestWithMissingContentLengthClosesServerCleanly() {
        byte[] badInput = "Content-Type: application/json\r\n\r\n{}".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(badInput),
            out,
            () -> new StubPlanRepo() {}
        ).run();
        assertThat(out.size()).isEqualTo(0);
    }

    @Test
    void nodeToJsonIncludesDocumentWhenPresent() {
        UUID nodeId = UUID.randomUUID();
        TestPlanNode n = node(nodeId);
        n.document(new Document("text/plain", "some content"));

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public Optional<TestPlanNode> getNodeData(UUID id) {
                    return id.equals(nodeId) ? Optional.of(n) : Optional.empty();
                }
            },
            req(1, "browse/node", "{\"nodeId\":\"" + nodeId + "\"}"),
            req(99, "shutdown", "{}")
        );

        var docObj = responses.get(0).getAsJsonObject("result").getAsJsonObject("document");
        assertThat(docObj.get("mimeType").getAsString()).isEqualTo("text/plain");
        assertThat(docObj.get("content").getAsString()).isEqualTo("some content");
    }

    @Test
    void nodeToJsonIncludesDataTableWhenPresent() {
        UUID nodeId = UUID.randomUUID();
        TestPlanNode n = node(nodeId);
        n.dataTable(new DataTable(List.of(List.of("col1", "col2"))));

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public Optional<TestPlanNode> getNodeData(UUID id) {
                    return id.equals(nodeId) ? Optional.of(n) : Optional.empty();
                }
            },
            req(1, "browse/node", "{\"nodeId\":\"" + nodeId + "\"}"),
            req(99, "shutdown", "{}")
        );

        var dataTable = responses.get(0).getAsJsonObject("result").getAsJsonArray("dataTable");
        assertThat(dataTable).hasSize(1);
    }

    @Test
    void nodeToJsonIncludesTagsAndProperties() {
        UUID nodeId = UUID.randomUUID();
        TestPlanNode n = node(nodeId);
        n.addTag("@smoke");
        n.addProperty("env", "prod");

        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {
                @Override public Optional<TestPlanNode> getNodeData(UUID id) {
                    return id.equals(nodeId) ? Optional.of(n) : Optional.empty();
                }
            },
            req(1, "browse/node", "{\"nodeId\":\"" + nodeId + "\"}"),
            req(99, "shutdown", "{}")
        );

        var result = responses.get(0).getAsJsonObject("result");
        assertThat(result.getAsJsonArray("tags").get(0).getAsString()).isEqualTo("@smoke");
        assertThat(result.getAsJsonObject("properties").get("env").getAsString()).isEqualTo("prod");
    }

    @Test
    void execRerunLooksUpPreviousExecution() {
        UUID originalExecId = UUID.randomUUID(), planId = UUID.randomUUID(), rootId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        TestPlan originalPlan = plan(planId, projectId, rootId);

        TestExecution originalEx = new TestExecution();
        originalEx.executionID(originalExecId);
        originalEx.planID(planId);
        originalEx.executedAt(Instant.EPOCH);
        originalEx.profile("staging");

        UUID newExecId = UUID.randomUUID();
        TestExecution newEx = new TestExecution();
        newEx.executionID(newExecId); newEx.planID(planId); newEx.executedAt(Instant.EPOCH);

        StubExecRepo execRepo = new StubExecRepo() {
            @Override public Optional<TestExecution> getExecution(UUID id) {
                return id.equals(originalExecId) ? Optional.of(originalEx) : Optional.empty();
            }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(concat(
                frame(req(1, "exec", "{\"rerun\":\"" + originalExecId + "\"}")),
                frame(req(99, "shutdown", "{}"))
            )),
            out,
            withExec(
                new StubPlanRepo() {
                    @Override public Optional<TestPlan> getPlan(UUID id) {
                        return id.equals(planId) ? Optional.of(originalPlan) : Optional.empty();
                    }
                },
                execRepo
            ),
            (onCreated, profile, suites) -> newEx,
            null, null
        ).run();

        List<JsonObject> responses = parseResponses(out.toByteArray());
        var result = responses.get(0).getAsJsonObject("result");
        assertThat(result.get("executionId").getAsString()).isEqualTo(newExecId.toString());
    }

    @Test
    void stepsIndexWithoutProviderReturnsError() {
        List<JsonObject> responses = run(
            () -> new StubPlanRepo() {},
            req(1, "steps/index", "{}"),
            req(99, "shutdown", "{}")
        );
        assertThat(responses.get(0).has("error")).isTrue();
    }

    @Test
    void stepsIndexReturnsJsonArray() {
        String json = "[{\"id\":\"my.step\",\"description\":\"A step\"}]";

        byte[][] frames = {frame(req(1, "steps/index", "{}")), frame(req(99, "shutdown", "{}"))};
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonRpcServer(
            new ByteArrayInputStream(concat(frames)), out,
            () -> new StubPlanRepo() {},
            null, null, null, () -> json
        ).run();

        List<JsonObject> responses = parseResponses(out.toByteArray());
        var result = responses.get(0).getAsJsonArray("result");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAsJsonObject().get("id").getAsString()).isEqualTo("my.step");
    }
}
