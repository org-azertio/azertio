package org.azertio.cli;

import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioException;
import org.azertio.core.AzertioPluginManager;
import org.azertio.core.AzertioReporter;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.persistence.TestExecutionRepository;
import org.azertio.core.util.Log;
import picocli.CommandLine;

import java.util.UUID;

@CommandLine.Command(
    name = "report",
    description = "Generate reports for a given execution"
)
public final class ReportCommand extends AbstractCommand {

    private static final Log log = Log.of();

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    Args args;

    static final class Args {
        @CommandLine.Option(
            names = {"--execution-id"},
            description = "UUID of the execution to report on"
        )
        UUID executionId;

        @CommandLine.Option(
            names = {"--last-execution"},
            description = "Generate reports for the most recent execution",
            defaultValue = "false"
        )
        boolean lastExecution;
    }

    @Override
    protected void execute() {
        AzertioContext context = getContext();
        AzertioRuntime runtime = buildRuntime(context);
        TestExecutionRepository execRepo = runtime.getRepository(TestExecutionRepository.class);

        UUID resolvedId = resolveExecutionId(execRepo);

        new AzertioReporter(runtime).report(resolvedId);
        out().println("Reports generated for execution " + resolvedId);
    }

    private UUID resolveExecutionId(TestExecutionRepository execRepo) {
        if (args.lastExecution) {
            return execRepo.getLastExecutionId()
                .orElseThrow(() -> new AzertioException("No executions found"));
        }
        execRepo.getExecution(args.executionId)
            .orElseThrow(() -> new AzertioException("Execution {} not found", args.executionId));
        return args.executionId;
    }

    private AzertioRuntime buildRuntime(AzertioContext context) {
        if (!context.plugins().isEmpty()) {
            AzertioPluginManager pluginManager = new AzertioPluginManager(context.configuration());
            for (String plugin : context.plugins()) {
                try {
                    boolean result = pluginManager.installPlugin(plugin);
                    if (!result) {
                        throw new AzertioException("Failed to install plugin {}", plugin);
                    }
                } catch (Exception e) {
                    log.error(e, "Failed to install plugin {}", plugin);
                }
            }
        }
        return new AzertioRuntime(context.configuration()).withProfile(profile(parent.profile));
    }
}