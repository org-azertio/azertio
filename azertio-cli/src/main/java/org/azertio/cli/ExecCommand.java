package org.azertio.cli;

import com.google.gson.JsonObject;
import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioException;
import org.azertio.core.AzertioPluginManager;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.execution.ExecutionResult;
import org.azertio.core.execution.TestExecution;
import org.azertio.core.execution.TestPlanExecutor;
import org.azertio.core.persistence.TestExecutionRepository;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.util.Log;
import picocli.CommandLine;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@CommandLine.Command(
    name = "exec",
    description = "Install plugins, mount the test plan and execute it"
)
public final class ExecCommand extends AbstractCommand {

    private static final Log log = Log.of();

    @CommandLine.Option(
        names = {"--detach"},
        description = "Print executionID immediately and run execution in background",
        defaultValue = "false"
    )
    boolean detach;

    @CommandLine.Option(
        names = {"--json"},
        description = "Output result as JSON",
        defaultValue = "false"
    )
    boolean json;

    @Override
    protected void execute() {
        AzertioContext context = getContext();

        if (detach) {
            executeDetached(context);
        } else {
            executeAttached(context);
        }
    }

    private void executeAttached(AzertioContext context) {
        AzertioRuntime runtime = buildRuntime(context);
        TestPlan plan = buildPlan(context, runtime);
        TestExecution execution = new TestPlanExecutor(runtime).execute(plan.planID(), null);

        // TODO: step 4 - reports

        Optional<ExecutionResult> result = Optional.empty();
        if (execution.executionRootNodeID() != null) {
            TestExecutionRepository execRepo = runtime.getRepository(TestExecutionRepository.class);
            result = execRepo.getExecutionNodeResult(execution.executionRootNodeID());
        }
        String resultName = result.map(ExecutionResult::name).orElse("-");

        if (json) {
            JsonObject obj = new JsonObject();
            obj.addProperty("executionId", execution.executionID().toString());
            obj.addProperty("result", resultName);
            System.out.println(obj);
        } else {
            System.out.println(execution.executionID() + " " + resultName);
        }
    }

    private void executeDetached(AzertioContext context) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<UUID> executionIdRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Thread bgThread = new Thread(() -> {
            try {
                AzertioRuntime runtime = buildRuntime(context);
                TestPlan plan = buildPlan(context, runtime);
                new TestPlanExecutor(runtime).execute(plan.planID(), id -> {
                    executionIdRef.set(id);
                    latch.countDown();
                });
                // TODO: step 4 - reports
            } catch (Throwable t) {
                errorRef.set(t);
                latch.countDown();
            }
        }, "azertio-exec");
        bgThread.setDaemon(false);
        bgThread.start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AzertioException("Interrupted while waiting for execution to start");
        }

        if (errorRef.get() != null) {
            Throwable t = errorRef.get();
            throw new AzertioException("Execution failed to start: {}", t.getMessage());
        }

        // Prevent System.exit() so the background thread can finish
        MainCommand.detachModeActive = true;

        if (json) {
            JsonObject obj = new JsonObject();
            obj.addProperty("executionId", executionIdRef.get().toString());
            System.out.println(obj);
        } else {
            System.out.println(executionIdRef.get());
        }
    }

    private AzertioRuntime buildRuntime(AzertioContext context) {
        // Step 1: install plugins
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

    private TestPlan buildPlan(AzertioContext context, AzertioRuntime runtime) {
        // Step 2: mount plan
        try {
            return runtime.buildTestPlan(context, getSelectedSuites());
        } catch (Exception e) {
            throw new AzertioException(e, "Failed to build test plan: {}", e.getMessage());
        }
    }
}
