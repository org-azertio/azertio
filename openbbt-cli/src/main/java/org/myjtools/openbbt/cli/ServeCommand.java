package org.myjtools.openbbt.cli;

import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.jsonrpc.serve.JsonRpcServer;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTPluginManager;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.execution.TestExecution;
import org.myjtools.openbbt.core.execution.TestPlanExecutor;
import org.myjtools.openbbt.core.persistence.AttachmentRepository;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.util.Log;
import picocli.CommandLine;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@CommandLine.Command(
    name = "serve",
    description = "Start OpenBBT in server mode, serving JSON-RPC 2.0 requests over stdio"
)
public final class ServeCommand extends AbstractCommand {

    private static final Log log = Log.of();

    @Override
    protected void execute() {
        LogConfig.redirectToFile(Path.of(System.getProperty("user.home"), ".openbbt", "openbbt.log"));
        OpenBBTContext context = getContext();
        Config inputParams = Config.ofMap(parent.params == null ? Map.of() : parent.params);

        // Single full-mode runtime shared by both the repository factory and the
        // exec handler. Using repositoryOnly() would open HSQLDB in read-only mode,
        // which would prevent exec from writing execution records.
        OpenBBTRuntime runtime = new OpenBBTRuntime(context.configuration());

        JsonRpcServer.ExecHandler execHandler = new JsonRpcServer.ExecHandler() {
            @Override
            public TestExecution exec(java.util.function.BiConsumer<UUID, UUID> onExecutionCreated, String profileName, List<String> suites) {
                if (!context.plugins().isEmpty()) {
                    OpenBBTPluginManager pluginManager = new OpenBBTPluginManager(context.configuration());
                    for (String plugin : context.plugins()) {
                        try {
                            pluginManager.installPlugin(plugin);
                        } catch (Exception e) {
                            log.error(e, "Failed to install plugin {}", plugin);
                        }
                    }
                }
                OpenBBTContext execContext = suites.isEmpty()
                    ? context
                    : readConfigurationFile().createContext(inputParams, suites);
                TestPlan plan;
                OpenBBTRuntime execRuntime = runtime.withProfile(profile(profileName));
                try {
                    plan = execRuntime.buildTestPlan(execContext, suites);
                } catch (Exception e) {
                    throw new OpenBBTException(e, "Failed to build test plan: {}", e.getMessage());
                }
                final var planId = plan.planID();
                Consumer<UUID> cb = onExecutionCreated != null
                    ? id -> onExecutionCreated.accept(id, planId)
                    : null;
                return new TestPlanExecutor(execRuntime).execute(planId, cb);
            }

            @Override
            public TestExecution rerun(java.util.function.BiConsumer<UUID, UUID> onExecutionCreated, UUID planID, String profileName) {
                OpenBBTRuntime execRuntime = runtime.withProfile(profile(profileName));
                Consumer<UUID> cb = onExecutionCreated != null
                    ? id -> onExecutionCreated.accept(id, planID)
                    : null;
                return new TestPlanExecutor(execRuntime).execute(planID, cb);
            }
        };

        JsonRpcServer.PlanHandler planHandler = () -> {
            try {
                return runtime.buildTestPlan(context, List.of());
            } catch (Exception e) {
                throw new OpenBBTException(e, "Failed to build test plan: {}", e.getMessage());
            }
        };

        new JsonRpcServer(System.in, System.out, new JsonRpcServer.RepositoryFactory() {
            @Override public TestPlanRepository open() {
                return runtime.getRepository(TestPlanRepository.class);
            }
            @Override public TestExecutionRepository openExecution() {
                return runtime.getRepository(TestExecutionRepository.class);
            }
            @Override public AttachmentRepository openAttachment() {
                return runtime.getRepository(AttachmentRepository.class);
            }
        }, execHandler, planHandler, runtime::getContributors, runtime::getStepIndex).run();
    }
}
