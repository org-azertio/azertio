package org.azertio.cli;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.execution.TestExecutionNode;
import org.azertio.core.persistence.TestExecutionRepository;
import picocli.CommandLine;

import java.util.UUID;

@CommandLine.Command(
    name = "get-execution-node",
    description = "Get information about an execution node given executionID and planNodeID"
)
public final class GetExecutionNodeCommand extends AbstractCommand {

    @CommandLine.Option(
        names = {"--execution-id"},
        description = "UUID of the test execution",
        required = true
    )
    UUID executionId;

    @CommandLine.Option(
        names = {"--plan-node-id"},
        description = "UUID of the plan node",
        required = true
    )
    UUID planNodeId;

    @CommandLine.Option(
        names = {"--json"},
        description = "Output as JSON",
        defaultValue = "false"
    )
    boolean json;

    @Override
    protected void execute() {
        TestExecutionRepository repository = AzertioRuntime
            .repositoryOnly(getContext().configuration())
            .getRepository(TestExecutionRepository.class);

        TestExecutionNode node = repository.getExecutionNode(executionId, planNodeId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Execution node not found for executionId=" + executionId + " planNodeId=" + planNodeId));

        if (json) {
            JsonObject obj = new JsonObject();
            obj.addProperty("executionNodeId", node.executionNodeID().toString());
            obj.addProperty("executionId",     node.executionID().toString());
            obj.addProperty("planNodeId",      node.planNodeID().toString());
            obj.add("result",      node.result()    != null ? new JsonPrimitive(node.result().name())                    : JsonNull.INSTANCE);
            obj.add("startedAt",   node.startTime() != null ? new JsonPrimitive(node.startTime().toString())             : JsonNull.INSTANCE);
            obj.add("finishedAt",  node.endTime()   != null ? new JsonPrimitive(node.endTime().toString())               : JsonNull.INSTANCE);
            obj.add("durationMs",  node.startTime() != null && node.endTime() != null ? new JsonPrimitive(node.duration()) : JsonNull.INSTANCE);
            obj.add("message",     node.message()   != null ? new JsonPrimitive(node.message())                          : JsonNull.INSTANCE);
            out().println(obj);
        } else {
            out().println("executionNodeId : " + node.executionNodeID());
            out().println("executionId     : " + node.executionID());
            out().println("planNodeId      : " + node.planNodeID());
            out().println("result          : " + (node.result()    != null ? node.result().name()      : "-"));
            out().println("startedAt       : " + (node.startTime() != null ? node.startTime()         : "-"));
            out().println("finishedAt      : " + (node.endTime()   != null ? node.endTime()           : "-"));
            out().println("durationMs      : " + (node.startTime() != null && node.endTime() != null ? node.duration() + "ms" : "-"));
            out().println("message         : " + (node.message()   != null ? node.message()           : "-"));
        }
    }
}
