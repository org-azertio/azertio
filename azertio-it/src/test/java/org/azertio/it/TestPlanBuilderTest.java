package org.azertio.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioFile;
import org.azertio.core.AzertioPluginManager;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.NodeType;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestPlanNode;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestPlanBuilderTest {

	private static final String ENV_PATH = "target/.azertio-it-planbuilder";

	@BeforeAll
	static void installPlugins() throws IOException {
		AzertioFile file = AzertioFile.read(new FileReader("src/test/resources/azertio.yaml"));
		var context = file.createContext(
			Config.ofMap(Map.of(AzertioConfig.ENV_PATH, ENV_PATH)),
			List.of()
		);
		AzertioPluginManager pluginManager = new AzertioPluginManager(context.configuration());
		for (String plugin : context.plugins()) {
			pluginManager.installPlugin(plugin);
		}
	}

	@Test
	void buildTestPlan_withNoSuitesDefined_createsDefaultSuiteWithAllScenarios(@TempDir Path tempDir) throws IOException {
		AzertioFile file = AzertioFile.read(new FileReader("src/test/resources/azertio-no-suites.yaml"));
		var context = file.createContext(
			Config.ofMap(Map.of(
				AzertioConfig.ENV_PATH, ENV_PATH,
				AzertioConfig.RESOURCE_PATH, "src/test/resources/test-features",
				AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_TRANSIENT
			)),
			List.of()
		);

		AzertioRuntime runtime = new AzertioRuntime(context.configuration());
		TestPlan testPlan = runtime.buildTestPlan(context);
		TestPlanRepository repo = runtime.getRepository(TestPlanRepository.class);

		// root should have exactly one child: the default TEST_SUITE
		List<UUID> suiteIDs = repo.getNodeChildren(testPlan.planNodeRoot()).toList();
		assertThat(suiteIDs).hasSize(1);

		TestPlanNode suite = repo.getNodeData(suiteIDs.get(0)).orElseThrow();
		assertThat(suite.nodeType()).isEqualTo(NodeType.TEST_SUITE);
		assertThat(suite.name()).isEqualTo("default");

		// the default suite must include all test cases from the feature files
		long testCases = repo.getNodeDescendants(suiteIDs.get(0))
			.map(id -> repo.getNodeData(id).orElseThrow())
			.filter(n -> n.nodeType() == NodeType.TEST_CASE)
			.count();
		assertThat(testCases).isGreaterThan(0);
	}

	@Test
	void buildTestPlan_reusesExistingPlanOnSecondCall(@TempDir Path tempDir) throws IOException {
		AzertioFile file = AzertioFile.read(new FileReader("src/test/resources/azertio.yaml"));
		var context = file.createContext(
			Config.ofMap(Map.of(
				AzertioConfig.ENV_PATH, ENV_PATH,
				AzertioConfig.RESOURCE_PATH, "src/test/resources/test-features",
				AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_FILE,
				AzertioConfig.PERSISTENCE_FILE, tempDir.resolve("plan.db").toString()
			)),
			List.of("suiteA")
		);

		// Step 1: first call creates a new plan
		TestPlan testPlan1 = new AzertioRuntime(context.configuration()).buildTestPlan(context);
		assertThat(testPlan1.planID()).isNotNull();

		// Step 2: second call reuses the existing plan
		TestPlan testPlan2 = new AzertioRuntime(context.configuration()).buildTestPlan(context);
		assertThat(testPlan2.planID()).isEqualTo(testPlan1.planID());
	}

}
