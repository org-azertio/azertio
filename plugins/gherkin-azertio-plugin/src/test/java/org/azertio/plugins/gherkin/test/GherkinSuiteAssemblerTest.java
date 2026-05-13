package org.azertio.plugins.gherkin.test;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.*;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.persistence.TestPlanRepositoryWriter;
import org.azertio.core.contributors.SuiteAssembler;
import org.azertio.core.testplan.NodeType;
import org.azertio.core.testplan.TagExpression;
import org.azertio.core.testplan.TestSuite;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class GherkinSuiteAssemblerTest {

	@Test
	void testEmptySuite() {
		AzertioRuntime cm = new AzertioRuntime(Config.ofMap(Map.of(
			AzertioConfig.ENV_PATH, "target/.azertio",
			AzertioConfig.RESOURCE_PATH, "src/test/resources/test-empty-suite",
			AzertioConfig.RESOURCE_FILTER, "**/*.feature",
			AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_TRANSIENT
		)));
		var planAssembler = cm.getExtensions(SuiteAssembler.class).findFirst().orElseThrow();
		TestSuite testSuite = new TestSuite("Test Suite", "Test Suite", TagExpression.EMPTY);
		assertThat(planAssembler.assembleSuite(testSuite)).isEmpty();
	}

	@Test
	void testSingleFeatureSuite() throws IOException {
		String string = assembleSuite("src/test/resources/test-single-feature");
		assertThat(string).hasToString("""
				[TEST_SUITE] Test Suite
				  [TEST_FEATURE] Test 1 - Simple Scenario
				    [TEST_CASE] Test1_Scenario1 - Test Scenario
				      [STEP_AGGREGATOR] Background
				        [STEP] Given the set of real numbers
				      [STEP] Given a number with value 8.02 and another number with value 9
				      [STEP] When both numbers are multiplied
				      [STEP] Then the matchResult is equals to 72.18
				""");
	}


	@Test
	void testComplexTestSuite() throws IOException {
		String string = assembleSuite("src/test/resources/test-features");
		assertThat(string).hasToString("""
				[TEST_SUITE] Test Suite
				  [TEST_FEATURE] Test - Scenario with arguments
				    [TEST_CASE] Test Scenario with document
				      [STEP] Given a number with value 8.02 and another number with value 9
				      [STEP] When both numbers are multiplied
				      [STEP] Then the matchResult is equals to:
				    [TEST_CASE] Test Scenario with data table
				      [STEP] Given a number with value 8.02 and another number with value 9
				      [STEP] When both numbers are multiplied
				      [STEP] Then the matchResult is equals to:
				  [TEST_FEATURE] Test 3 - Backgrounds
				    [TEST_CASE] Test3_Scenario1 - Test Scenario 1
				      [STEP_AGGREGATOR] Background
				        [STEP] Given the set of real numbers ℝ
				      [STEP] Given a number with value 8.02 and another number with value 9
				      [STEP] When both numbers are multiplied
				      [STEP] Then the matchResult is equals to 72.18
				    [TEST_CASE] Test3_Scenario2 - Test Scenario 2
				      [STEP_AGGREGATOR] Background
				        [STEP] Given the set of real numbers ℝ
				      [STEP] Given a number with value 7.02 and another number with value 8
				      [STEP] When both numbers are multiplied
				      [STEP] Then the matchResult is equals to 56.16
				    [TEST_CASE] Test3_Scenario3 - Test Scenario 3
				      [STEP_AGGREGATOR] Background
				        [STEP] Given the set of real numbers ℝ
				      [STEP] Given a number with value 8.09 and another number with value 9
				      [STEP] When both numbers are multiplied
				      [STEP] Then the matchResult is equals to 72.81
				  [TEST_FEATURE] Test 2 - Scenario Outline
				    [TEST_FEATURE] Test Scenario Outline
				      [TEST_CASE] ScenarioOutline1_1 - Test Scenario Outline [1]
				        [STEP] Given a number with value 1.0 and another number with value 2
				        [STEP] When both numbers are multiplied
				        [STEP] Then the matchResult is equals to 2.0
				      [TEST_CASE] ScenarioOutline1_2 - Test Scenario Outline [2]
				        [STEP] Given a number with value 2.0 and another number with value 3
				        [STEP] When both numbers are multiplied
				        [STEP] Then the matchResult is equals to 6.0
				      [TEST_CASE] ScenarioOutline1_3 - Test Scenario Outline [3]
				        [STEP] Given a number with value 5.0 and another number with value 4
				        [STEP] When both numbers are multiplied
				        [STEP] Then the matchResult is equals to 20.0
				  [TEST_FEATURE] Test 1 - Simple Scenario
				    [TEST_CASE] Test1_Scenario1 - Test Scenario
				      [STEP] Given a number with value 8.02 and another number with value 9
				      [STEP] When both numbers are multiplied
				      [STEP] Then the matchResult is equals to 72.18
				""");
	}


	@Test
	void testRedefiningSuite() throws IOException {
		String string = assembleSuite("src/test/resources/test-features-redefining");
		System.out.println(string);
		assertThat(string).hasToString("""
				[TEST_SUITE] Test Suite
				  [TEST_FEATURE] Test Redefined feature - Definition
				    [TEST_CASE] 1 - Test scenario
				      [STEP_AGGREGATOR] Antecedentes
				        [STEP] Dado el conjunto de números reales ℝ
				      [STEP_AGGREGATOR] Given two numbers
				        [STEP] Dado un número con valor 6,1 y otro número con valor 3
				        [STEP] Y un número con valor 6,1 y otro número con valor 3
				      [STEP_AGGREGATOR] When they are multiplied
				        [STEP] Cuando se multiplican ambos números
				      [STEP_AGGREGATOR] Then the matchResult is the product
				        [STEP] Entonces el resultado es 18,3
				        [STEP] Y el resultado es 18,3
				      [VIRTUAL_STEP] And the world is wonderful
				    [TEST_FEATURE] Test scenario outline
				      [TEST_CASE] 2_1 - Test scenario outline [1]
				        [STEP_AGGREGATOR] Antecedentes
				          [STEP] Dado el conjunto de números reales ℝ
				        [VIRTUAL_STEP] Given a number with value 1.0
				        [STEP_AGGREGATOR] And another number with value 2
				          [STEP] Dado un número con valor 1.0 y otro número con valor 2
				        [STEP_AGGREGATOR] When they are multiplied
				          [STEP] Cuando se multiplican ambos números
				        [STEP_AGGREGATOR] Then the matchResult is 2.0
				          [STEP] Entonces el resultado es 2.0
				          [STEP] Y el resultado es 2.0
				      [TEST_CASE] 2_2 - Test scenario outline [2]
				        [STEP_AGGREGATOR] Antecedentes
				          [STEP] Dado el conjunto de números reales ℝ
				        [VIRTUAL_STEP] Given a number with value 2.0
				        [STEP_AGGREGATOR] And another number with value 4
				          [STEP] Dado un número con valor 2.0 y otro número con valor 4
				        [STEP_AGGREGATOR] When they are multiplied
				          [STEP] Cuando se multiplican ambos números
				        [STEP_AGGREGATOR] Then the matchResult is 8.0
				          [STEP] Entonces el resultado es 8.0
				          [STEP] Y el resultado es 8.0
				      [TEST_CASE] 2_3 - Test scenario outline [3]
				        [STEP_AGGREGATOR] Antecedentes
				          [STEP] Dado el conjunto de números reales ℝ
				        [VIRTUAL_STEP] Given a number with value 3.0
				        [STEP_AGGREGATOR] And another number with value 6
				          [STEP] Dado un número con valor 3.0 y otro número con valor 6
				        [STEP_AGGREGATOR] When they are multiplied
				          [STEP] Cuando se multiplican ambos números
				        [STEP_AGGREGATOR] Then the matchResult is 18.0
				          [STEP] Entonces el resultado es 18.0
				          [STEP] Y el resultado es 18.0
				""");



	}

	@Test
	void testScenarioOutlineSubstitutesPlaceholdersInDocStringsAndDataTables() {
		AssembledSuite suite = assembleSuiteData("src/test/resources/test-outline-arguments");
		TestPlanRepository repository = suite.repository();

		var nodes = repository.getNodeDescendants(suite.rootNode()).toList().stream()
			.map(id -> repository.getNodeData(id).orElseThrow())
			.toList();

		assertThat(nodes)
			.filteredOn(node -> node.nodeType() == NodeType.STEP && node.document() != null)
			.extracting(node -> node.document().content())
			.containsExactly(
				"{\n  \"name\": \"Alice\",\n  \"age\": 31\n}",
				"{\n  \"name\": \"Bob\",\n  \"age\": 42\n}"
			);

		assertThat(nodes)
			.filteredOn(node -> node.nodeType() == NodeType.STEP && node.dataTable() != null)
			.extracting(node -> node.dataTable().values())
			.containsExactly(
				java.util.List.of(
					java.util.List.of("name", "age"),
					java.util.List.of("Alice", "31")
				),
				java.util.List.of(
					java.util.List.of("name", "age"),
					java.util.List.of("Bob", "42")
				)
			);
	}

	private String assembleSuite(String path) throws IOException {
		AssembledSuite suite = assembleSuiteData(path);
		TestPlanRepository repository = suite.repository();
		TestPlanRepositoryWriter writer = new TestPlanRepositoryWriter(repository);
		StringBuilder output = new StringBuilder();
		writer.write(suite.rootNode(), output::append);
		return output.toString();
	}

	private AssembledSuite assembleSuiteData(String path) {
		AzertioRuntime cm = new AzertioRuntime(Config.ofMap(Map.of(
			AzertioConfig.ENV_PATH, "target/.azertio",
			AzertioConfig.RESOURCE_PATH, path,
			AzertioConfig.RESOURCE_FILTER, "**/*.feature",
			AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_TRANSIENT
		)));
		var planAssembler = cm.getExtensions(SuiteAssembler.class).findFirst().orElseThrow();
		TestSuite testSuite = new TestSuite("Test Suite", "Test Suite", TagExpression.EMPTY);
		UUID rootNode = planAssembler.assembleSuite(testSuite).orElseThrow();
		TestPlanRepository repository = cm.getRepository(TestPlanRepository.class);
		return new AssembledSuite(rootNode, repository);
	}

	private record AssembledSuite(UUID rootNode, TestPlanRepository repository) {}

}
