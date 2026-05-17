package org.azertio.plugins.gherkin.test;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.gherkinparser.GherkinParser;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.persistence.TestPlanRepositoryWriter;
import org.azertio.persistence.DataSourceProvider;
import org.azertio.persistence.plan.JooqPlanRepository;
import org.azertio.core.testplan.TagExpression;
import org.azertio.plugins.gherkin.FeaturePlanAssembler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class FeaturePlanAssemblerTest {

	@TempDir
	Path tempDir;

	@Test
	void testAssemble() throws IOException {

		TestPlanRepository repository = new JooqPlanRepository(DataSourceProvider.h2fileLocal(tempDir.resolve("testdb")));
		var writer = new TestPlanRepositoryWriter(repository);
		Optional<UUID> testPlan = assembleFeature(repository,"");
		assertThat(testPlan).isPresent();
		StringBuilder output = new StringBuilder();
		writer.write(testPlan.orElseThrow(), output::append);

		assertThat(output).hasToString("""
			[TEST_FEATURE] Test 1 - Simple Scenario
			  [TEST_CASE] Test1_Scenario1 - Test Scenario
			    [STEP_AGGREGATOR] Background
			      [STEP] Given the set of real numbers
			    [STEP] Given a number with value 8.02 and another number with value 9
			    [STEP] When both numbers are multiplied
			    [STEP] Then the matchResult is equals to 72.18
			  [TEST_FEATURE] Test Scenario Outline
			    [TEST_CASE] Test1_ScenarioOutline_1 - Test Scenario Outline [1]
			      [STEP_AGGREGATOR] Background
			        [STEP] Given the set of real numbers
			      [STEP] Given a number with value 8.02 and another number with value 9
			      [STEP] When both numbers are multiplied
			      [STEP] Then the matchResult is equals to 72.18
			    [TEST_CASE] Test1_ScenarioOutline_2 - Test Scenario Outline [2]
			      [STEP_AGGREGATOR] Background
			        [STEP] Given the set of real numbers
			      [STEP] Given a number with value 5 and another number with value 4
			      [STEP] When both numbers are multiplied
			      [STEP] Then the matchResult is equals to 20
			""");

	}

	@Test
	void testAssembleScenarioWithTags() throws IOException {

		TestPlanRepository repository = new JooqPlanRepository(DataSourceProvider.h2fileLocal(tempDir.resolve("testdb")));
		var writer = new TestPlanRepositoryWriter(repository);
		Optional<UUID> testPlan = assembleFeature(repository,"ScenarioA");
		assertThat(testPlan).isPresent();
		StringBuilder output = new StringBuilder();
		writer.write(testPlan.orElseThrow(), output::append);

		assertThat(output).hasToString("""
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
	void testAssembleScenarioOutlineWithTags() throws IOException {

		TestPlanRepository repository = new JooqPlanRepository(DataSourceProvider.h2fileLocal(tempDir.resolve("testdb")));
		var writer = new TestPlanRepositoryWriter(repository);
		Optional<UUID> testPlan = assembleFeature(repository,"ScenarioB");
		assertThat(testPlan).isPresent();
		StringBuilder output = new StringBuilder();
		writer.write(testPlan.orElseThrow(), output::append);

		assertThat(output).hasToString("""
			[TEST_FEATURE] Test 1 - Simple Scenario
			  [TEST_FEATURE] Test Scenario Outline
			    [TEST_CASE] Test1_ScenarioOutline_1 - Test Scenario Outline [1]
			      [STEP_AGGREGATOR] Background
			        [STEP] Given the set of real numbers
			      [STEP] Given a number with value 8.02 and another number with value 9
			      [STEP] When both numbers are multiplied
			      [STEP] Then the matchResult is equals to 72.18
			    [TEST_CASE] Test1_ScenarioOutline_2 - Test Scenario Outline [2]
			      [STEP_AGGREGATOR] Background
			        [STEP] Given the set of real numbers
			      [STEP] Given a number with value 5 and another number with value 4
			      [STEP] When both numbers are multiplied
			      [STEP] Then the matchResult is equals to 20
			""");

	}

	@Test
	void testAssembleScenarioWithInvalidTags() throws IOException {
		TestPlanRepository repository = new JooqPlanRepository(DataSourceProvider.h2fileLocal(tempDir.resolve("testdb")));
		Optional<UUID> testPlan = assembleFeature(repository,"InvalidTag");
		assertThat(testPlan).isEmpty();
	}


	@Test
	void testTagsAndPropertiesAreInheritedFromParentToChild() throws IOException {
		TestPlanRepository repository = new JooqPlanRepository(DataSourceProvider.h2fileLocal(tempDir.resolve("testdb")));
		UUID featureId = assembleFeature(repository, "").orElseThrow();

		// Feature: own tags and properties
		assertThat(repository.getNodeTags(featureId)).contains("Test1");
		assertThat(repository.getNodeProperties(featureId)).containsEntry("featureProperty", "A");

		// Scenario: inherits feature tags and properties, adds its own
		UUID scenarioId = repository.getNodeChildren(featureId).findFirst().orElseThrow();
		assertThat(repository.getNodeTags(scenarioId)).contains("Test1", "ScenarioA");
		assertThat(repository.getNodeProperties(scenarioId))
			.containsEntry("featureProperty", "A")
			.containsEntry("scenarioProperty", "B");

		// Background aggregator: inherits scenario tags and properties
		UUID backgroundId = repository.getNodeChildren(scenarioId).findFirst().orElseThrow();
		assertThat(repository.getNodeTags(backgroundId)).contains("Test1", "ScenarioA");
		assertThat(repository.getNodeProperties(backgroundId))
			.containsEntry("featureProperty", "A")
			.containsEntry("scenarioProperty", "B");

		// Background step: STEP nodes do NOT inherit tags or properties from parent
		UUID backgroundStepId = repository.getNodeChildren(backgroundId).findFirst().orElseThrow();
		assertThat(repository.getNodeTags(backgroundStepId)).isEmpty();
		assertThat(repository.getNodeProperties(backgroundStepId))
			.containsOnlyKeys("gherkinType")
			.doesNotContainKey("featureProperty")
			.doesNotContainKey("scenarioProperty");

		// First scenario step: STEP nodes do NOT inherit tags or properties from parent,
		// but do have properties declared in their own comments
		UUID firstStepId = repository.getNodeChildren(scenarioId).skip(1).findFirst().orElseThrow();
		assertThat(repository.getNodeTags(firstStepId)).isEmpty();
		assertThat(repository.getNodeProperties(firstStepId))
			.containsEntry("stepProperty", "C")
			.doesNotContainKey("featureProperty")
			.doesNotContainKey("scenarioProperty");
	}


	private Optional<UUID> assembleFeature (TestPlanRepository planNodeRepository, String tagExpression) throws IOException {
		GherkinParser parser = new GherkinParser(new DefaultKeywordMapProvider());
		var gherkinDocument = parser.parse(Files.newInputStream(Path.of("src/test/resources/test1/simpleScenario.feature")));
		var feature = gherkinDocument.feature();

		FeaturePlanAssembler assembler = new FeaturePlanAssembler(
			feature,
			"/path/to/feature",
			new DefaultKeywordMapProvider(),
			"ID-(\\w+)",
			planNodeRepository,
			TagExpression.parse(tagExpression)
		);

		return assembler.createTestPlan();
	}

}
