package org.azertio.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.execution.ExecutionResult;
import org.azertio.core.execution.TestExecution;
import org.azertio.core.persistence.TestExecutionRepository;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.TestPlan;
import picocli.CommandLine;

import java.util.List;
import java.util.UUID;

@CommandLine.Command(
    name = "list-executions",
    description = "List executions for a given test plan, ordered by date descending"
)
public final class ListExecutionsCommand extends AbstractCommand {

    @CommandLine.Option(
        names = {"--plan-id"},
        description = "UUID of the test plan",
        required = true
    )
    UUID planId;

    @CommandLine.Option(
        names = {"--offset"},
        description = "Number of records to skip (default: 0)",
        defaultValue = "0"
    )
    int offset;

    @CommandLine.Option(
        names = {"--max"},
        description = "Maximum number of records to return, 0 means no limit (default: 0)",
        defaultValue = "0"
    )
    int max;

    @CommandLine.Option(
        names = {"--json"},
        description = "Output as JSON array",
        defaultValue = "false"
    )
    boolean json;

    @Override
    protected void execute() {
        AzertioRuntime runtime = AzertioRuntime.repositoryOnly(getContext().configuration());
        TestPlanRepository planRepository = runtime.getRepository(TestPlanRepository.class);
        TestExecutionRepository repository = runtime.getRepository(TestExecutionRepository.class);
        var planOpt = planRepository.getPlan(planId);
        if (planOpt.isEmpty()) {
            if (json) out().println("[]");
            return;
        }
        List<TestExecution> executions = repository.listExecutions(planId, planOpt.get().planNodeRoot(), offset, max);
        if (json) {
            JsonArray result = new JsonArray();
            for (TestExecution ex : executions) {
                JsonObject obj = new JsonObject();
                obj.addProperty("executionId", ex.executionID().toString());
                obj.addProperty("planId", ex.planID().toString());
                obj.add("executionRootNodeId", ex.executionRootNodeID() != null
                    ? com.google.gson.JsonParser.parseString("\"" + ex.executionRootNodeID() + "\"")
                    : JsonNull.INSTANCE);
                obj.addProperty("executedAt", ex.executedAt().toString());
                result.add(obj);
            }
            out().println(result);
        } else {
            for (TestExecution ex : executions) {
                String result = resolveResult(repository, ex);
                out().println(ex.executionID() + " " + ex.executedAt() + " " + result);
            }
        }
    }

    private String resolveResult(TestExecutionRepository repository, TestExecution ex) {
        if (ex.executionRootNodeID() == null) return "-";
        return repository.getExecutionNodeResult(ex.executionRootNodeID())
            .map(ExecutionResult::name)
            .orElse("-");
    }
}
