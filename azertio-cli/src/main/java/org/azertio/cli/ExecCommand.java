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
import org.azertio.core.util.AnsiColors;
import org.azertio.core.util.Log;
import picocli.CommandLine;

import java.time.Duration;
import java.time.Instant;
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

    @CommandLine.Option(
        names = {"--exit-zero"},
        description = "Always exit with code 0, even if tests failed",
        defaultValue = "false"
    )
    boolean exitZero;

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
        Instant startTime = Instant.now();

        TestExecution execution = new TestPlanExecutor(runtime).execute(plan.planID(), id -> {
            if (json) {
                JsonObject obj = new JsonObject();
                obj.addProperty("executionId", id.toString());
                out().println(obj);
            }
        });

        Duration elapsed = Duration.between(startTime, Instant.now());

        Optional<ExecutionResult> result = Optional.empty();
        if (execution.executionRootNodeID() != null) {
            TestExecutionRepository execRepo = runtime.getRepository(TestExecutionRepository.class);
            result = execRepo.getExecutionNodeResult(execution.executionRootNodeID());
        }
        String resultName = result.map(ExecutionResult::name).orElse("-");

        if (json) {
            JsonObject obj = new JsonObject();
            obj.addProperty("result", resultName);
            obj.addProperty("executionId", execution.executionID().toString());
            obj.addProperty("passed", execution.testPassedCount());
            obj.addProperty("failed", execution.testFailedCount());
            obj.addProperty("error", execution.testErrorCount());
            obj.addProperty("durationMs", elapsed.toMillis());
            out().println(obj);
        } else {
            printSummary(execution, resultName, elapsed);
        }

        boolean passed = ExecutionResult.PASSED.name().equals(resultName);
        if (!passed && !exitZero) {
            exitCode = 1;
        }
    }

    private void printSummary(TestExecution execution, String resultName, Duration elapsed) {
        int passed  = execution.testPassedCount() != null ? execution.testPassedCount() : 0;
        int failed  = execution.testFailedCount() != null ? execution.testFailedCount() : 0;
        int error   = execution.testErrorCount()  != null ? execution.testErrorCount()  : 0;
        int total   = passed + failed + error;

        String resultColor = "PASSED".equals(resultName) ? AnsiColors.GREEN : AnsiColors.RED;
        out().println(AnsiColors.color(resultName, resultColor));
        String passedStr = AnsiColors.color(passed + " passed", AnsiColors.GREEN);
        String failedStr = failed > 0 ? AnsiColors.color(failed + " failed", AnsiColors.RED) : (failed + " failed");
        String errorStr  = error  > 0 ? AnsiColors.color(error  + " error",  AnsiColors.RED) : (error  + " error");
        out().printf("Tests: %d total, %s, %s, %s  |  Time: %s%n",
            total, passedStr, failedStr, errorStr, formatDuration(elapsed));
        out().println("ExecutionID: " + execution.executionID());
    }

    private static String formatDuration(Duration d) {
        long ms = d.toMillis();
        if (ms < 1000) return ms + "ms";
        return String.format("%.3fs", ms / 1000.0);
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
            out().println(obj);
        } else {
            out().println(executionIdRef.get());
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