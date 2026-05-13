package org.azertio.plugins.gherkin;


import java.util.*;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.gherkinparser.GherkinParser;
import org.myjtools.gherkinparser.elements.Examples;
import org.myjtools.gherkinparser.elements.Feature;
import org.myjtools.gherkinparser.elements.ScenarioOutline;
import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.PostConstruct;
import org.azertio.core.*;
import org.azertio.core.persistence.TestPlanNodeCriteria;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.contributors.SuiteAssembler;
import org.azertio.core.testplan.NodeType;
import org.azertio.core.testplan.TestPlanNode;
import org.azertio.core.testplan.TagExpression;
import org.azertio.core.testplan.TestSuite;
import org.azertio.core.util.Log;
import java.io.IOException;
import java.util.stream.Stream;
import static org.azertio.plugins.gherkin.GherkinConstants.*;

/**
 * SPI implementation that assembles a test suite from Gherkin {@code .feature} files.
 *
 * <p>This assembler discovers all {@code .feature} resources in the project, parses them
 * using the Gherkin parser, and builds a {@link TestPlanNode} tree that represents the test
 * suite. When multiple feature files are present, it also handles the
 * <em>definition/implementation redefining</em> mechanism: features tagged with
 * {@code @definition} provide abstract test case structures, while features tagged with
 * {@code @implementation} supply the concrete steps that replace them.</p>
 *
 * <p>The redefining process works as follows:</p>
 * <ol>
 *   <li>All features are parsed independently into plan nodes.</li>
 *   <li>Definition test cases without an identifier are removed.</li>
 *   <li>Implementation scenario outlines are filled with examples from their
 *       matching definition counterparts.</li>
 *   <li>Each definition test case is redefined with the steps from its matching
 *       implementation test case (matched by identifier), using the optional
 *       {@code gherkin.step-map} property to control step mapping.</li>
 *   <li>Implementation-only features are removed from the final tree.</li>
 * </ol>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see FeaturePlanAssembler
 */
@Extension
public class GherkinSuiteAssembler implements SuiteAssembler {

	private static final Log log = Log.of("plugins.gherkin");
	private static final String STEP_MAP = "gherkin.step-map";

	@Inject("gherkin")
	Config config;

	@Inject
	TestPlanRepository repository;

	@Inject
	ResourceSet resourceSet;

	private DefaultKeywordMapProvider keywordMapProvider;
	private GherkinParser parser;
	private String idTagPattern;
	private String definitionTag;
	private String implementationTag;
	private final Map<UUID,Object> underlyingModels = new HashMap<>();


	/**
	 * Initializes the Gherkin parser and reads configuration values for identifier tag
	 * pattern, definition tag, and implementation tag. Called automatically after
	 * dependency injection.
	 */
	@PostConstruct
	public void init() {
		this.keywordMapProvider = new DefaultKeywordMapProvider();
		this.parser = new GherkinParser(keywordMapProvider);
		this.idTagPattern = config.getString(AzertioConfig.ID_TAG_PATTERN).orElseThrow();
		this.definitionTag = config.getString(AzertioConfig.DEFINITION_TAG).orElseThrow();
		this.implementationTag = config.getString(AzertioConfig.IMPLEMENTATION_TAG).orElseThrow();
	}


	/**
	 * {@inheritDoc}
	 *
	 * <p>Discovers all {@code *.feature} resources and assembles them into a plan node tree.
	 * When a single feature is found, it is wrapped directly in a test suite node. When
	 * multiple features are found, the definition/implementation redefining mechanism is
	 * applied before producing the final suite.</p>
	 */
	@Override
	public Optional<UUID> assembleSuite(TestSuite testSuite) {
		var resources = resourceSet.filter(resource -> "feature".equals(resource.extension())).toList();
		if (resources.size() == 1) {
			var feature = assembleFeatureNode(keywordMapProvider, parser, idTagPattern, resources.get(0), testSuite);
			return feature.flatMap(it -> wrapTestSuite(it, testSuite));
		} else {
			return assembleMultipleFeature(resources, testSuite);
		}
	}


	private Optional<UUID> wrapTestSuite(UUID feature, TestSuite testSuite) {
		TestPlanNode root = new TestPlanNode(NodeType.TEST_SUITE);
		root.name(testSuite.name());
		var id = repository.persistNode(root);
		repository.attachChildNodeLast(id, feature);
		return Optional.of(id);
	}


	private Optional<UUID> assembleMultipleFeature(List<Resource> resources, TestSuite testSuite) {

		var root = assembleStandaloneFeatures(resources, testSuite);

		deleteDefinitionTestCasesWithoutId(root);
		deleteImplementationScenarioOutlineContent(root);
		fillImplementationScenarioOutlines(root, testSuite.tagExpression());

		// redefine definition test cases with implementation steps
		repository.searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(root),
			TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			TestPlanNodeCriteria.withTag(definitionTag)
		)).forEach(definitionTestCase -> redefine(root,definitionTestCase));

		repository.searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(root),
			TestPlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_FEATURE),
			TestPlanNodeCriteria.withTag(implementationTag)
		)).forEach(implementationFeature -> repository.deleteNode(implementationFeature));

		repository.searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(root),
			TestPlanNodeCriteria.or(
				TestPlanNodeCriteria.withTag(definitionTag),
				TestPlanNodeCriteria.withTag(implementationTag)
			)
		)).forEach(it -> {
			repository.removeNodeTag(it, implementationTag);
			repository.removeNodeTag(it, definitionTag);
		});

		if (repository.countNodeChildren(root) == 0) {
			repository.deleteNode(root);
			return Optional.empty();
		} else {
			repository.updateNodeField(root, "nodeType", NodeType.TEST_SUITE.value);
			if (testSuite.description() != null && !testSuite.description().isBlank()) {
				repository.updateNodeField(root, "name", testSuite.description());
			} else {
				repository.updateNodeField(root, "name", testSuite.name());
			}
			return Optional.ofNullable(root);
		}

	}


	private void deleteImplementationScenarioOutlineContent(UUID root) {
		log.trace("deleteImplementationScenarioOutlineContent");
		repository.searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(root),
			TestPlanNodeCriteria.withTag(implementationTag),
			TestPlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE)
		)).forEach(scenarioOutline ->
			repository.getNodeChildren(scenarioOutline).toList()
				.forEach(child -> repository.deleteNode(child))
		);
	}




	private UUID assembleStandaloneFeatures(List<Resource> resources, TestSuite testSuite) {
		log.trace("provideStandaloneFeatures");
		var id = repository.persistNode(new TestPlanNode(NodeType.TEST_FEATURE));
		for (Resource resource : resources) {
			assembleFeatureNode(
				keywordMapProvider,
				parser,
				idTagPattern,
				resource,
				testSuite
			).ifPresent(child -> repository.attachChildNodeLast(id, child));
		}
		return id;
	}


	private void deleteDefinitionTestCasesWithoutId(UUID root) {
		repository.searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(root),
			TestPlanNodeCriteria.withTag(definitionTag),
			TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			TestPlanNodeCriteria.withField("identifier", null)
		)).forEach(it -> repository.deleteNode(it));
	}


	private void fillImplementationScenarioOutlines(UUID root, TagExpression tagExpression) {
		log.trace("fillImplementationScenarioOutlines");
		repository.searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(root),
			TestPlanNodeCriteria.withTag(implementationTag),
			TestPlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE)
		)).forEach(scenarioOutline -> fillImplementationScenarioOutline(root, scenarioOutline, tagExpression));
	}



	private void fillImplementationScenarioOutline(UUID root, UUID impScenarioOutline, TagExpression tagExpression) {

		UUID impFeature = repository.getParentNode(impScenarioOutline).orElseThrow();
		String identifier = repository.getNodeField(impScenarioOutline, "identifier").orElseThrow().toString();

		UUID defScenarioOutline = repository.searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(root),
			TestPlanNodeCriteria.withNodeType(NodeType.TEST_FEATURE),
			TestPlanNodeCriteria.withTag(definitionTag),
			TestPlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE),
			TestPlanNodeCriteria.withField("identifier",identifier)
		)).findFirst().orElseThrow(
			() -> new AzertioException(
				"There is no definition feature with name {}",
				repository.getNodeField(impFeature, "name").orElseThrow()
			)
		);

		Examples defExamples = ((ScenarioOutline) underlyingModels.get(defScenarioOutline))
			.examples()
			.getFirst();

		var stepsFromExamples = new FeaturePlanAssembler(
			(Feature) underlyingModels.get(impFeature),
			repository.getNodeField(impFeature, "source").map(Object::toString).orElse(""),
			keywordMapProvider,
			idTagPattern,
			repository,
			tagExpression
		).createScenariosFromExamples(
			(ScenarioOutline) underlyingModels.get(impScenarioOutline),
			defExamples,
			impScenarioOutline
		);

		stepsFromExamples.forEach(step -> repository.attachChildNodeLast(impScenarioOutline, step));

	}



	private void redefine(UUID root, UUID defTestCase) {
		log.trace("redefine {}",defTestCase);
		implementationTestCase(root, defTestCase).ifPresentOrElse(
			impTestCase -> redefineTestCase(defTestCase, impTestCase),
			() -> repository.deleteNode(defTestCase)
		);
	}


	private void redefineTestCase(UUID defTestCase, UUID impTestCase) {

		// definition background is ignored
		deleteBackground(defTestCase);

		int[] stepMap = extractStepMap(defTestCase, impTestCase);

		var impSteps = repository.getNodeChildren(impTestCase)
			.filter(id -> repository.getNodeData(id)
				.map(n -> n.nodeType() == NodeType.STEP)
				.orElse(false))
			.toList();


		int defStepCount = 0;
		int impStepCount = 0;

		for (UUID defStep : repository.getNodeChildren(defTestCase).toList()) {
			for (int i = 0; i < stepMap[defStepCount]; i++) {
				UUID impStep = impSteps.get(impStepCount);
				repository.getParentNode(impStep).ifPresent(impStepParent -> repository.detachChildNode(impStepParent, impStep));
				repository.attachChildNodeLast(defStep,impStep);
				impStepCount++;
			}
			redefineStepNodeType(stepMap, defStepCount, defStep);
			defStepCount++;
		}

		moveBackgroundToOtherTestCase(impTestCase, defTestCase);

	}



	private void redefineStepNodeType(int[] stepMap, int defStepCount, UUID defStep) {
		if (stepMap[defStepCount] == 0) {
			repository.updateNodeField(defStep, "nodeType", NodeType.VIRTUAL_STEP.value);
		} else {
			repository.updateNodeField(defStep, "nodeType", NodeType.STEP_AGGREGATOR.value);
		}
	}


	private void moveBackgroundToOtherTestCase(UUID origin, UUID target) {
		repository.searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.childOf(origin),
			TestPlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_BACKGROUND)
		)).findFirst().ifPresent(originBackground -> {
			repository.detachChildNode(origin, originBackground);
			repository.attachChildNodeFirst(target, originBackground); // background always in first
			repository.updateNodeField(originBackground, "name", "<definition>");
			}
		);
	}


	private int[] extractStepMap(UUID defTestCase, UUID impTestCase) {
		// step map is in form: x-x-x-x...
		String stepMapProperty = repository.getNodeProperty(impTestCase, STEP_MAP).orElse(null);
		if (stepMapProperty == null || stepMapProperty.isBlank()) {
			// if not defiend, map 1-to-1 for each step
			int defTestCaseChildren = repository.countNodeChildren(defTestCase);
			stepMapProperty = "-1".repeat(defTestCaseChildren).substring(1);
		}
		return Stream.of(stepMapProperty.split("-")).mapToInt(Integer::parseInt).toArray();
	}


	private void deleteBackground(UUID defTestCase) {
		repository.getNodeChildren(defTestCase)
			.filter(child -> repository.existsNodeProperty(child, GHERKIN_TYPE, GHERKIN_TYPE_BACKGROUND))
			.forEach( child -> repository.deleteNode(child) );
	}


	private Optional<UUID> implementationTestCase(UUID root, UUID definitionTestCase) {
		return repository.searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(root),
			TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			TestPlanNodeCriteria.withTag(implementationTag),
			TestPlanNodeCriteria.withField("identifier",repository.getNodeField(definitionTestCase,"identifier").orElseThrow()))
		).findFirst();
	}



	private Optional<UUID> assembleFeatureNode(
		DefaultKeywordMapProvider keywordMapProvider,
		GherkinParser parser,
		String idTagPattern,
		Resource resource,
		TestSuite testSuite
	) {

		try (var inputStream = resource.open()) {
			var gherkinDocument = parser.parse(inputStream);
			if (gherkinDocument.feature() == null) {
				return Optional.empty();
			}
			var builder = new FeaturePlanAssembler(
				gherkinDocument.feature(),
				resource.relativePath().toString(),
				keywordMapProvider,
				idTagPattern,
				repository,
				testSuite.tagExpression()
			);
			var result = builder.createTestPlan();
			this.underlyingModels.putAll(builder.underlyingModels());
			return result;
		} catch (RuntimeException | IOException e) {
			log.error(e,"Cannot read resource {}",resource.relativePath());
			return Optional.empty();
		}

	}


}