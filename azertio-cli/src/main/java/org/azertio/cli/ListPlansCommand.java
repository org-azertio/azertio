package org.azertio.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.TestPlan;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(
    name = "list-plans",
    description = "List test plans for a given organization and project"
)
public final class ListPlansCommand extends AbstractCommand {

    @CommandLine.Option(
        names = {"--organization", "-o"},
        description = "Organization name",
        required = true
    )
    String organization;

    @CommandLine.Option(
        names = {"--project"},
        description = "Project name",
        required = true
    )
    String project;

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
        TestPlanRepository repository = AzertioRuntime
            .repositoryOnly(getContext().configuration())
            .getRepository(TestPlanRepository.class);
        List<TestPlan> plans = repository.listPlans(organization, project, offset, max);
        if (json) {
            JsonArray result = new JsonArray();
            for (TestPlan plan : plans) {
                JsonObject obj = new JsonObject();
                obj.addProperty("planId", plan.planID().toString());
                obj.addProperty("createdAt", plan.createdAt().toString());
                boolean hasIssues = repository.getNodeData(plan.planNodeRoot())
                    .map(n -> n.hasIssues()).orElse(false);
                obj.addProperty("hasIssues", hasIssues);
                result.add(obj);
            }
            out().println(result);
        } else {
            for (TestPlan plan : plans) {
                boolean hasIssues = repository.getNodeData(plan.planNodeRoot())
                    .map(n -> n.hasIssues()).orElse(false);
                out().println(plan.planID() + " " + plan.createdAt() + " " + (hasIssues ? "ISSUES" : "OK"));
            }
        }
    }
}
