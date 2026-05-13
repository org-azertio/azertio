package org.azertio.it;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.azertio.core.contributors.SuiteAssembler;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.*;
import java.util.Optional;
import java.util.UUID;

/**
 * Test-only SuiteAssembler that builds specific plan trees based on the suite name,
 * allowing validation tests to control the tree structure without needing real test resources.
 */
@Extension
public class TestTreeSuiteAssembler implements SuiteAssembler {

	@Inject
	TestPlanRepository repository;

	@Override
	public Optional<UUID> assembleSuite(TestSuite testSuite) {
		return switch (testSuite.name()) {
			case "validStep"                    -> suiteWithStep("a valid step");
			case "invalidStep"                  -> suiteWithStep("this step does not exist");
			case "testCaseNoSteps"              -> suiteWithTestCaseNoSteps();
			case "stepAggregatorNoStepChildren" -> suiteWithEmptyAggregator();
			case "execPassingStep"              -> suiteWithStep("a valid step");
			case "execFailingStep"              -> suiteWithStep("a failing step");
			case "execErrorStep"                -> suiteWithStep("an error step");
			case "execUndefinedStep"            -> suiteWithStep("this step does not exist");
			case "execVirtualStep"              -> suiteWithVirtualStep();
			case "execTwoTestCases"             -> suiteWithTwoTestCases();
			case "execMixedResults"             -> suiteWithMixedResults();
			case "execBenchmarkMode"            -> suiteWithBenchmarkMode();
			default                             -> Optional.empty();
		};
	}

	// TEST_SUITE → TEST_FEATURE → TEST_CASE → STEP_AGGREGATOR → STEP(stepName)
	private Optional<UUID> suiteWithStep(String stepName) {
		UUID suite      = node(NodeType.TEST_SUITE,       "suite");
		UUID feature    = node(NodeType.TEST_FEATURE,     "feature");
		UUID testCase   = node(NodeType.TEST_CASE,        "test case");
		UUID aggregator = node(NodeType.STEP_AGGREGATOR,  "steps");
		UUID step       = node(NodeType.STEP,             stepName);

		repository.attachChildNodeLast(suite, feature);
		repository.attachChildNodeLast(feature, testCase);
		repository.attachChildNodeLast(testCase, aggregator);
		repository.attachChildNodeLast(aggregator, step);
		return Optional.of(suite);
	}

	// TEST_SUITE → TEST_FEATURE → TEST_CASE  (no steps at all)
	private Optional<UUID> suiteWithTestCaseNoSteps() {
		UUID suite    = node(NodeType.TEST_SUITE,   "suite");
		UUID feature  = node(NodeType.TEST_FEATURE, "feature");
		UUID testCase = node(NodeType.TEST_CASE,    "test case with no steps");

		repository.attachChildNodeLast(suite, feature);
		repository.attachChildNodeLast(feature, testCase);
		return Optional.of(suite);
	}

	// TEST_SUITE → TEST_FEATURE → TEST_CASE → STEP_AGGREGATOR  (aggregator has no STEP children)
	private Optional<UUID> suiteWithEmptyAggregator() {
		UUID suite      = node(NodeType.TEST_SUITE,      "suite");
		UUID feature    = node(NodeType.TEST_FEATURE,    "feature");
		UUID testCase   = node(NodeType.TEST_CASE,       "test case");
		UUID aggregator = node(NodeType.STEP_AGGREGATOR, "empty aggregator");

		repository.attachChildNodeLast(suite, feature);
		repository.attachChildNodeLast(feature, testCase);
		repository.attachChildNodeLast(testCase, aggregator);
		return Optional.of(suite);
	}

	// TEST_SUITE → TEST_FEATURE → TEST_CASE → STEP_AGGREGATOR → STEP("a valid step")
	//                                                         → VIRTUAL_STEP
	private Optional<UUID> suiteWithVirtualStep() {
		UUID suite       = node(NodeType.TEST_SUITE,       "suite");
		UUID feature     = node(NodeType.TEST_FEATURE,     "feature");
		UUID testCase    = node(NodeType.TEST_CASE,        "test case");
		UUID aggregator  = node(NodeType.STEP_AGGREGATOR,  "steps");
		UUID step        = node(NodeType.STEP,             "a valid step");
		UUID virtualStep = node(NodeType.VIRTUAL_STEP,     "virtual step");

		repository.attachChildNodeLast(suite, feature);
		repository.attachChildNodeLast(feature, testCase);
		repository.attachChildNodeLast(testCase, aggregator);
		repository.attachChildNodeLast(aggregator, step);
		repository.attachChildNodeLast(aggregator, virtualStep);
		return Optional.of(suite);
	}

	// TEST_SUITE → TEST_FEATURE → TEST_CASE_1 → STEP_AGGREGATOR → STEP
	//                           → TEST_CASE_2 → STEP_AGGREGATOR → STEP
	private Optional<UUID> suiteWithTwoTestCases() {
		UUID suite    = node(NodeType.TEST_SUITE,   "suite");
		UUID feature  = node(NodeType.TEST_FEATURE, "feature");
		UUID testCase1 = node(NodeType.TEST_CASE,   "test case 1");
		UUID aggregator1 = node(NodeType.STEP_AGGREGATOR, "steps");
		UUID step1    = node(NodeType.STEP,         "a valid step");
		UUID testCase2 = node(NodeType.TEST_CASE,   "test case 2");
		UUID aggregator2 = node(NodeType.STEP_AGGREGATOR, "steps");
		UUID step2    = node(NodeType.STEP,         "a valid step");

		repository.attachChildNodeLast(suite, feature);
		repository.attachChildNodeLast(feature, testCase1);
		repository.attachChildNodeLast(testCase1, aggregator1);
		repository.attachChildNodeLast(aggregator1, step1);
		repository.attachChildNodeLast(feature, testCase2);
		repository.attachChildNodeLast(testCase2, aggregator2);
		repository.attachChildNodeLast(aggregator2, step2);
		return Optional.of(suite);
	}

	// TEST_SUITE → TEST_FEATURE → TEST_CASE_1 → STEP_AGGREGATOR → STEP("a valid step")    [PASSED]
	//                           → TEST_CASE_2 → STEP_AGGREGATOR → STEP("a failing step")  [FAILED]
	//                           → TEST_CASE_3 → STEP_AGGREGATOR → STEP("an error step")   [ERROR]
	private Optional<UUID> suiteWithMixedResults() {
		UUID suite    = node(NodeType.TEST_SUITE,   "suite");
		UUID feature  = node(NodeType.TEST_FEATURE, "feature");

		UUID testCase1    = node(NodeType.TEST_CASE,        "passing test case");
		UUID aggregator1  = node(NodeType.STEP_AGGREGATOR,  "steps");
		UUID step1        = node(NodeType.STEP,             "a valid step");

		UUID testCase2    = node(NodeType.TEST_CASE,        "failing test case");
		UUID aggregator2  = node(NodeType.STEP_AGGREGATOR,  "steps");
		UUID step2        = node(NodeType.STEP,             "a failing step");

		UUID testCase3    = node(NodeType.TEST_CASE,        "error test case");
		UUID aggregator3  = node(NodeType.STEP_AGGREGATOR,  "steps");
		UUID step3        = node(NodeType.STEP,             "an error step");

		repository.attachChildNodeLast(suite,      feature);
		repository.attachChildNodeLast(feature,    testCase1);
		repository.attachChildNodeLast(testCase1,  aggregator1);
		repository.attachChildNodeLast(aggregator1, step1);
		repository.attachChildNodeLast(feature,    testCase2);
		repository.attachChildNodeLast(testCase2,  aggregator2);
		repository.attachChildNodeLast(aggregator2, step2);
		repository.attachChildNodeLast(feature,    testCase3);
		repository.attachChildNodeLast(testCase3,  aggregator3);
		repository.attachChildNodeLast(aggregator3, step3);
		return Optional.of(suite);
	}

	// TEST_SUITE → TEST_FEATURE → TEST_CASE → STEP_AGGREGATOR
	//    → STEP("benchmark mode is enabled with 5 executions and 2 threads")
	//    → STEP("a benchmarkable step")
	//    → STEP("the benchmark mean response time (ms) is greater than or equal to 0")
	private Optional<UUID> suiteWithBenchmarkMode() {
		UUID suite       = node(NodeType.TEST_SUITE,      "suite");
		UUID feature     = node(NodeType.TEST_FEATURE,    "feature");
		UUID testCase    = node(NodeType.TEST_CASE,       "benchmark test");
		UUID aggregator  = node(NodeType.STEP_AGGREGATOR, "steps");
		UUID enableStep  = node(NodeType.STEP, "benchmark mode is enabled with 500 executions and 10 threads");
		UUID benchStep   = node(NodeType.STEP, "a benchmarkable step");
		UUID assertStep  = node(NodeType.STEP, "the benchmark mean response time (ms) is greater than or equal to 0");

		repository.attachChildNodeLast(suite, feature);
		repository.attachChildNodeLast(feature, testCase);
		repository.attachChildNodeLast(testCase, aggregator);
		repository.attachChildNodeLast(aggregator, enableStep);
		repository.attachChildNodeLast(aggregator, benchStep);
		repository.attachChildNodeLast(aggregator, assertStep);
		return Optional.of(suite);
	}

	private UUID node(NodeType type, String name) {
		return repository.persistNode(new TestPlanNode(type).name(name));
	}
}