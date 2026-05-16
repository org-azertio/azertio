package org.azertio.core.testplan;

import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioException;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.backend.StepProviderBackend;
import org.azertio.core.contributors.SuiteAssembler;
import org.azertio.core.contributors.TestPlanValidator;
import org.azertio.core.events.TestPlanCreated;
import org.azertio.core.persistence.TestPlanNodeCriteria;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.util.Hash;
import org.azertio.core.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlanBuilder {

	private static final Log log = Log.of();

	private final AzertioRuntime runtime;

	public PlanBuilder(AzertioRuntime runtime) {
		this.runtime = runtime;
	}


	/**
	 * Builds a test plan for the given context by assembling the test plan nodes and
	 * registering the plan in the repository.
	 * If a plan with the same resource set and configuration already exists, it will be reused.
	 * @param context the context for which the test plan should be built
	 * @return the generated PlanID of the registered test plan
	 * @throws AzertioException if the test plan could not be assembled or registered
	 */
	public TestPlan buildTestPlan(AzertioContext context) {
		return buildTestPlan(context, List.of());
	}


	public TestPlan buildTestPlan(AzertioContext context, List<String> selectedSuites) {


		TestPlanRepository testPlanRepository = runtime.getRepository(TestPlanRepository.class);

		// create project if not exists
		UUID projectID = testPlanRepository.persistProject(context.testProject());

		String resourceSetHash = runtime.resourceSet().hash();
		String configurationHash = Hash.of(runtime.configuration().toString() + context.testSuites().toString());

		TestPlan testPlan = testPlanRepository.getPlan(context.testProject(), resourceSetHash, configurationHash).orElse(null);
		if (testPlan == null) {
			// No existing plan found, assemble a new one
			var rootNodeID = assembleTestPlanNodes(context).orElseThrow(
				() -> new AzertioException("Failed to assemble test plan for project: {}", context.testProject().name())
			);
			int testCaseCount = testPlanRepository.countNodes(
				TestPlanNodeCriteria.and(
					TestPlanNodeCriteria.descendantOf(rootNodeID),
					TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE)
				)
			);
			String suites = selectedSuites.isEmpty()
				? null
				: String.join(",", selectedSuites);
			testPlan = new TestPlan(
				null,
				projectID,
				runtime.clock().now(),
				resourceSetHash,
				configurationHash,
				rootNodeID,
				testCaseCount,
				suites
			);
			testPlan = testPlanRepository.persistPlan(testPlan);
			testPlanRepository.assignPlanToNodes(testPlan.planID(), rootNodeID);
			testPlanRepository.assignTestCaseCountsToNodes(testPlan.planID());
			var backend = new StepProviderBackend(runtime);
			log.debug("Validating test plan");
			for (var validator : runtime.getExtensions(TestPlanValidator.class).toList()) {
				validator.validate(testPlan, backend);
			}
			var rootNode = testPlanRepository.getNodeData(rootNodeID).orElseThrow();
			if (rootNode.hasIssues()) {
				testPlanRepository.getNodeDescendantsWithIssues(rootNodeID).forEach(id -> {
					var nodeWithIssues = testPlanRepository.getNodeData(id).orElseThrow();
					log.warn(
						"Validation issue in '{}' ({}): {}",
						nodeWithIssues.name(),
						nodeWithIssues.source(),
						nodeWithIssues.validationMessage()
					);
				});
			} else {
				log.info("Test plan validated successfully with no issues: {}", testPlan.planID());
			}
			log.debug("Registered new test plan: {}", testPlan.planID());
			runtime.eventBus().publish(
				new TestPlanCreated(runtime.clock().now(),testPlan.projectID(),testPlan.planID(),rootNode.hasIssues())
			);
		} else {
			log.debug("Reusing existing test plan: {}", testPlan.planID());
		}
		return testPlan;
	}



	/*
	 * Assembles the test plan for the given context by invoking all registered SuiteAssemblers.
	 */
	private Optional<UUID> assembleTestPlanNodes(AzertioContext context) {
		TestPlanRepository planNodeRepository = runtime.getRepository(TestPlanRepository.class);
		List<SuiteAssembler> assemblers = runtime.getExtensions(SuiteAssembler.class).toList();
		if (assemblers.isEmpty()) {
			log.warn("No SuiteAssembler found, cannot assemble test plan");
			return Optional.empty();
		}
		List<UUID> nodes = new ArrayList<>();
		for (String suiteName : context.testSuites()) {
			TestSuite testSuite = context.testSuite(suiteName).orElseThrow(
					() -> new AzertioException("Test suite not found in project: {}", suiteName)
			);
			for (SuiteAssembler assembler : assemblers) {
				assembler.assembleSuite(testSuite).ifPresent(nodes::add);
			}
		}
		if (nodes.isEmpty()) {
			log.warn("No test plan nodes assembled for test suites: {}", context.testSuites());
			return Optional.empty();
		}
		TestPlanNode root = new TestPlanNode(NodeType.TEST_PLAN);
		root.name("Test Plan");
		var rootID = planNodeRepository.persistNode(root);
		for (UUID nodeId : nodes) {
			planNodeRepository.attachChildNodeLast(rootID, nodeId);
		}
		return Optional.ofNullable(rootID);
	}


}
