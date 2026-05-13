package org.azertio.test;

import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.execution.TestExecution;
import org.azertio.core.execution.TestPlanExecutor;
import org.azertio.core.testplan.TagExpression;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestProject;
import org.azertio.core.testplan.TestSuite;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test fixture injected by {@link AzertioExtension} into JUnit 5 test methods.
 * <p>
 * Holds the resolved feature directory and temp directory, accepts plugin-specific
 * configuration, and drives a full {@link TestPlanExecutor} run when {@link #execute()}
 * is called.
 *
 * <pre>{@code
 * @Test
 * @FeatureDir("get-200")
 * void myTest(JUnitAzertioPlan plan) {
 *     plan.withConfig("rest.baseURL", "http://localhost:" + port)
 *         .execute()
 *         .assertAllPassed();
 * }
 * }</pre>
 */
public class JUnitAzertioPlan {

    private final Path featureDirPath;
    private final Path tempDir;
    private final Map<String, String> extraConfig = new LinkedHashMap<>();

    JUnitAzertioPlan(Path featureDirPath, Path tempDir) {
        this.featureDirPath = featureDirPath;
        this.tempDir = tempDir;
    }

    public JUnitAzertioPlan withConfig(String key, String value) {
        extraConfig.put(key, value);
        return this;
    }

    public JUnitAzertioResult execute() {
        Map<String, String> configMap = new LinkedHashMap<>();
        configMap.put(AzertioConfig.ENV_PATH,         tempDir.toString());
        configMap.put(AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_FILE);
        configMap.put(AzertioConfig.PERSISTENCE_FILE, tempDir.resolve("test.db").toString());
        configMap.put(AzertioConfig.RESOURCE_PATH,    featureDirPath.toString());
        configMap.put(AzertioConfig.RESOURCE_FILTER,  "**/*");
        configMap.putAll(extraConfig);

        Config config = Config.ofMap(configMap);
        AzertioRuntime runtime = new AzertioRuntime(config);

        String suiteName = featureDirPath.getFileName().toString();
        TestSuite suite = new TestSuite(suiteName, "", TagExpression.EMPTY);
        TestProject project = new TestProject("Azertio Test", "", "", List.of(suite));
        AzertioContext context = new AzertioContext(project, config, List.of(suiteName), List.of());

        TestPlan plan = runtime.buildTestPlan(context);
        TestExecution execution = new TestPlanExecutor(runtime).execute(plan.planID());

        return new JUnitAzertioResult(runtime, plan, execution);
    }
}
