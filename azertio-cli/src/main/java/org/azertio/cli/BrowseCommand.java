package org.azertio.cli;

import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.persistence.TestPlanFormatter;
import org.azertio.core.persistence.TestPlanHierarchyFormatter;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestPlanNode;
import org.azertio.core.util.Log;
import picocli.CommandLine;

import java.util.UUID;

@CommandLine.Command(
    name = "browse",
    description = "Browse the content of an existing test plan"
)
public final class BrowseCommand extends AbstractCommand {

    private static final Log log = Log.of();

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    Target target;

    static class Target {
        @CommandLine.Option(
            names = {"--plan"},
            description = "UUID of the test plan to browse from its root"
        )
        String planID;

        @CommandLine.Option(
            names = {"--node"},
            description = "UUID of a node to browse from (use with --detail and --depth to navigate large trees)"
        )
        String nodeID;
    }

    @CommandLine.Option(
        names = {"--json"},
        description = "Output the test plan as hierarchical JSON (only with --plan)",
        defaultValue = "false"
    )
    boolean json;

    @CommandLine.Option(
        names = {"--depth"},
        description = "Maximum depth of the node tree. No limit by default.",
        defaultValue = "-1"
    )
    int depth;

    @Override
    protected void execute() {
        long t0 = System.currentTimeMillis();
        AzertioContext context = getContext();
        log.info("[t] getContext: {}ms", System.currentTimeMillis() - t0); t0 = System.currentTimeMillis();
        AzertioRuntime runtime = AzertioRuntime.repositoryOnly(context.configuration());
        log.info("[t] repositoryOnly: {}ms", System.currentTimeMillis() - t0); t0 = System.currentTimeMillis();
        TestPlanRepository repository = runtime.getRepository(TestPlanRepository.class);
        log.info("[t] getRepository: {}ms", System.currentTimeMillis() - t0); t0 = System.currentTimeMillis();
        try {
            if (target.planID != null) {
                executePlan(repository);
            } else {
                executeNode(repository);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void executePlan(TestPlanRepository repository) throws Exception {
        UUID uuid = UUID.fromString(target.planID);
        TestPlan testPlan = repository.getPlan(uuid)
            .orElseThrow(() -> new IllegalArgumentException("Test plan not found: " + target.planID));
        try {
            if (json) {
                new TestPlanHierarchyFormatter(repository, depth).format(testPlan, out()::print);
            } else {
                new TestPlanFormatter(repository, depth).format(testPlan, out()::print);
            }
        } catch (Exception e) {
            log.warn("Error formatting test plan: {}", e.getMessage());
        }
    }

    private void executeNode(TestPlanRepository repository) throws Exception {
        UUID uuid = UUID.fromString(target.nodeID);
        TestPlanNode node = repository.getNodeData(uuid)
            .orElseThrow(() -> new IllegalArgumentException("Node not found: " + target.nodeID));
        try {
            if (json) {
                new TestPlanHierarchyFormatter(repository, depth).formatFromNode(node, out()::print);
            } else {
                new TestPlanFormatter(repository, depth).formatFromNode(node, out()::print);
            }
        } catch (Exception e) {
            log.warn("Error formatting node: {}", e.getMessage());
        }
    }
}
