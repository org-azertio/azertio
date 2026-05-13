package org.azertio.plugins.markdownplan.test;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.contributors.SuiteAssembler;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.persistence.TestPlanRepositoryWriter;
import org.azertio.core.testplan.TagExpression;
import org.azertio.core.testplan.TestSuite;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownSuiteAssemblerTest {

	@Test
	void testEmptySuite() {
		AzertioRuntime cm = new AzertioRuntime(Config.ofMap(Map.of(
			AzertioConfig.ENV_PATH, "target/.azertio",
			AzertioConfig.RESOURCE_PATH, "src/test/resources/test-empty-suite",
			AzertioConfig.RESOURCE_FILTER, "**/*.md",
			AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_TRANSIENT
		)));
		var planAssembler = cm.getExtensions(SuiteAssembler.class).findFirst().orElseThrow();
		TestSuite testSuite = new TestSuite("Test Suite", "Test Suite", TagExpression.EMPTY);
		assertThat(planAssembler.assembleSuite(testSuite)).isEmpty();
	}

	@Test
	void testSimpleFeatureSuite() throws Exception {
		String output = assembleSuite("src/test/resources/test-simple-feature");
		assertThat(output).hasToString("""
				[TEST_SUITE] Test Suite
				  [TEST_FEATURE] Simple Feature - Test 1
				    [TEST_CASE] Test Case 1
				      [STEP] Given a number with value 8.02 and another number with value 9
				      [STEP] When both numbers are multiplied
				      [STEP] Then the matchResult is equals to 72.18
				""");
	}

	@Test
	void testComplexFeaturesSuite() throws Exception {
		String output = assembleSuite("src/test/resources/test-features");
		assertThat(output).hasToString("""
				[TEST_SUITE] Test Suite
				  [TEST_FEATURE] Simple Feature - Test 1
				    [TEST_CASE] Test Case 1
				      [STEP] Given a number with value 8.02 and another number with value 9
				      [STEP] When both numbers are multiplied
				      [STEP] Then the matchResult is equals to 72.18
				  [TEST_FEATURE] Feature with Arguments
				    [TEST_CASE] Test Case with Data Table
				      [STEP] Given a number with value 8.02 and another number with value 9
				      [STEP] When both numbers are multiplied
				      [STEP] Then the matchResult is equals to:
				    [TEST_CASE] Test Case with Document
				      [STEP] Given a number with value 8.02 and another number with value 9
				      [STEP] When both numbers are multiplied
				      [STEP] Then the matchResult is equals to:
				""");
	}

	@Test
	void testTagFiltering() throws Exception {
		String output = assembleSuite("src/test/resources/test-features-tags", "TagA");
		assertThat(output).hasToString("""
				[TEST_SUITE] Test Suite
				  [TEST_FEATURE] Tagged Feature
				    [TEST_CASE] Test Case A
				      [STEP] Given step given A
				      [STEP] When step when A
				""");
	}

	@Test
	void testTagFilteringNoMatch() {
		AzertioRuntime cm = new AzertioRuntime(Config.ofMap(Map.of(
			AzertioConfig.ENV_PATH, "target/.azertio",
			AzertioConfig.RESOURCE_PATH, "src/test/resources/test-features-tags",
			AzertioConfig.RESOURCE_FILTER, "**/*.md",
			AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_TRANSIENT
		)));
		var planAssembler = cm.getExtensions(SuiteAssembler.class).findFirst().orElseThrow();
		TestSuite testSuite = new TestSuite("Test Suite", "Test Suite", TagExpression.parse("InvalidTag"));
		assertThat(planAssembler.assembleSuite(testSuite)).isEmpty();
	}

	@Test
	void testMultipleH1InOneFile() throws Exception {
		String output = assembleSuite("src/test/resources/test-multiple-h1");
		assertThat(output).hasToString("""
				[TEST_SUITE] Test Suite
				  [TEST_FEATURE] multiSection.md
				    [TEST_FEATURE] Feature Alpha
				      [TEST_CASE] Test Case Alpha 1
				        [STEP] Given alpha step 1
				    [TEST_FEATURE] Feature Beta
				      [TEST_CASE] Test Case Beta 1
				        [STEP] Given beta step 1
				""");
	}


	private String assembleSuite(String path) throws Exception {
		return assembleSuite(path, TagExpression.EMPTY);
	}

	private String assembleSuite(String path, String tagExpression) throws Exception {
		return assembleSuite(path, TagExpression.parse(tagExpression));
	}

	private String assembleSuite(String path, TagExpression tagExpression) throws Exception {
		AzertioRuntime cm = new AzertioRuntime(Config.ofMap(Map.of(
			AzertioConfig.ENV_PATH, "target/.azertio",
			AzertioConfig.RESOURCE_PATH, path,
			AzertioConfig.RESOURCE_FILTER, "**/*.md",
			AzertioConfig.PERSISTENCE_MODE, AzertioConfig.PERSISTENCE_MODE_TRANSIENT
		)));
		var planAssembler = cm.getExtensions(SuiteAssembler.class).findFirst().orElseThrow();
		TestSuite testSuite = new TestSuite("Test Suite", "Test Suite", tagExpression);
		UUID planID = planAssembler.assembleSuite(testSuite).orElseThrow();
		TestPlanRepository repository = cm.getRepository(TestPlanRepository.class);
		TestPlanRepositoryWriter writer = new TestPlanRepositoryWriter(repository);
		StringBuilder output = new StringBuilder();
		writer.write(planID, output::append);
		return output.toString();
	}

}