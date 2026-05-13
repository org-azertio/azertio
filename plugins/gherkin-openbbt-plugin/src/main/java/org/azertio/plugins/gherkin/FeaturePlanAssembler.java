package org.azertio.plugins.gherkin;


import java.util.UUID;
import org.myjtools.gherkinparser.GherkinDialectFactory;
import org.myjtools.gherkinparser.KeywordMapProvider;
import org.myjtools.gherkinparser.KeywordType;
import org.myjtools.gherkinparser.elements.*;
import org.myjtools.gherkinparser.elements.DataTable;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.*;
import org.azertio.core.util.Patterns;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.azertio.plugins.gherkin.GherkinConstants.*;

/**
 * Transforms a parsed Gherkin {@link Feature} into a tree of
 * {@link org.azertio.core.testplan.TestPlanNode} elements persisted in a
 * {@link TestPlanRepository}.
 *
 * <p>Each Gherkin element is mapped to a plan node type:</p>
 * <ul>
 *   <li>{@code Feature} &rarr; {@link NodeType#TEST_FEATURE}</li>
 *   <li>{@code Scenario} &rarr; {@link NodeType#TEST_CASE}</li>
 *   <li>{@code Scenario Outline} &rarr; {@link NodeType#TEST_FEATURE} with child
 *       test cases expanded from its {@code Examples} table</li>
 *   <li>{@code Background} &rarr; {@link NodeType#STEP_AGGREGATOR}</li>
 *   <li>{@code Step} &rarr; {@link NodeType#STEP}</li>
 * </ul>
 *
 * <p>Tags, properties (extracted from Gherkin comments matching
 * {@code # key: value}), identifiers, and source locations are propagated to the
 * resulting plan nodes. A {@link TagExpression} filter is applied so that only
 * scenarios matching the expression are included in the output.</p>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see GherkinSuiteAssembler
 */
public class FeaturePlanAssembler {

	private final Pattern propertyRegex = Patterns.of("\\s*#+\\s*([^\\s]+)\\s*:\\s*([^\\s]+)\\s*");

	private final Feature feature;
	private final String relativePath;
	private final String scenarioKeyword;
	private final String backgroundKeyword;
	private final Background background;
	private final Pattern idTagPattern;
	private final TagExpression tagExpression;
	private final TestPlanRepository repository;
	private final Map<UUID, Object> underlyingModels = new HashMap<>();



	/**
	 * Creates a new assembler for the given feature.
	 *
	 * @param feature            the parsed Gherkin feature
	 * @param relativePath       the relative path of the {@code .feature} file, used for
	 *                           source location references
	 * @param keywordMapProvider provider for localized Gherkin keywords
	 * @param idTagPattern       regex pattern used to extract identifiers from tags
	 * @param repository         the repository where plan nodes are persisted
	 * @param tagExpression      expression used to filter scenarios by tags
	 */
	public FeaturePlanAssembler(
		Feature feature,
		String relativePath,
		KeywordMapProvider keywordMapProvider,
		String idTagPattern,
		TestPlanRepository repository,
		TagExpression tagExpression
	) {
		this.feature = feature;
		this.relativePath = relativePath;
		var dialect = new GherkinDialectFactory(keywordMapProvider,"en").dialectFor(feature.language());
		this.scenarioKeyword = dialect.keywords(KeywordType.SCENARIO).getFirst();
		this.backgroundKeyword = dialect.keywords(KeywordType.BACKGROUND).getFirst();
		this.repository = repository;
		this.background = feature.children().stream()
			.filter(Background.class::isInstance)
			.map(it -> (Background)it)
			.findFirst()
			.orElse(null);
		this.idTagPattern = Pattern.compile(idTagPattern);
		this.tagExpression = tagExpression;
	}


	/**
	 * Builds the plan node tree for the feature and returns the root node identifier.
	 *
	 * @return the persisted root node ID, or empty if the feature produced no scenarios
	 *         (e.g. all scenarios were filtered out by the tag expression)
	 */
	public Optional<UUID> createTestPlan() {
		return featureNode();
	}


	private Optional<UUID> featureNode() {

		var nodeData = new TestPlanNode(NodeType.TEST_FEATURE)
			.identifier(idFromTags(feature))
			.name(feature.name())
			.language(feature.language())
			.keyword(feature.keyword())
			.display("{name}")
			.description(feature.description())
			.tags(tags(feature))
			.source(nodeLocation(feature))
			.addProperties(propertiesFromComments(feature))
			.addProperty(GHERKIN_TYPE, GHERKIN_TYPE_FEATURE);

		var id = repository.persistNode(nodeData);
		underlyingModels.put(id, feature);

		for (var child : feature.children()) {
			if (child instanceof Scenario scenario) {
				scenarioNode(scenario,id).ifPresent( scenarioNode ->
					repository.attachChildNodeLast(id, scenarioNode)
				);
			} else if (child instanceof ScenarioOutline scenarioOutline) {
				scenarioOutlineNode(scenarioOutline,id).ifPresent( scenarioOutlineNode ->
					 repository.attachChildNodeLast(id, scenarioOutlineNode)
				);
			}
		}

		if (repository.countNodeChildren(id) == 0) {
			repository.deleteNode(id);
			underlyingModels.remove(id);
			return Optional.empty();
		}

		return Optional.of(id);
	}



	private Optional<UUID> scenarioNode(Scenario scenario, UUID parent) {
		return scenarioNode(scenario, scenario.name(), idFromTags(scenario), scenario.keyword(), parent);
	}


	private Optional<UUID> scenarioNode(ScenarioOutline scenarioOutline, int example, UUID parent) {
		return scenarioNode(
			scenarioOutline,
			"%s [%s]".formatted(scenarioOutline.name(), example),
			idFromTags(scenarioOutline)+"_"+example,
			scenarioKeyword,
			parent
		);
	}


	private Optional<UUID> scenarioNode(
		ScenarioDefinition scenarioDefinition,
		String name,
		String identifier,
		String keyword,
		UUID parent
	) {
		boolean include = tagExpression.evaluate(tags(parent, scenarioDefinition));
		if (!include) {
			return Optional.empty();
		}
		var data = new TestPlanNode(NodeType.TEST_CASE)
			.identifier(identifier)
			.name(name)
			.language(feature.language())
			.display(identifier != null ? "{identifier} - {name}" : "{name}")
			.keyword(keyword)
			.description(scenarioDefinition.description())
			.tags(tags(parent,scenarioDefinition))
			.source(nodeLocation(scenarioDefinition))
			.addProperties(propertiesFromComments(scenarioDefinition,parent))
			.addProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO);

		var id = repository.persistNode(data);
		underlyingModels.put(id,scenarioDefinition);

		var backgroundNodeId = createBackgroundStepsNode(id, null);
		if (backgroundNodeId != null) {
			repository.attachChildNodeLast(id, backgroundNodeId);
		}

		scenarioDefinition.children().forEach( step -> repository.attachChildNodeLast(id, stepNode(step, id)));
		return Optional.of(id);
	}


	private Optional<UUID> scenarioOutlineNode(ScenarioOutline scenarioOutline, UUID parent) {

		boolean include = tagExpression.evaluate(tags(parent, scenarioOutline));
		if (!include) {
			return Optional.empty();
		}
		var node = new TestPlanNode(NodeType.TEST_FEATURE)
			.identifier(idFromTags(scenarioOutline))
			.name(scenarioOutline.name())
			.display("{name}")
			.language(feature.language())
			.keyword(scenarioOutline.keyword())
			.description(scenarioOutline.description())
			.tags(tags(parent,scenarioOutline))
			.source(nodeLocation(scenarioOutline))
			.addProperties(propertiesFromComments(scenarioOutline,parent))
			.addProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE);

		node.dataTable(tableOf(scenarioOutline.examples().getFirst()));

		var id = repository.persistNode(node);
		underlyingModels.put(id,scenarioOutline);

		scenarioOutline.examples().stream()
			.flatMap(examples -> createScenariosFromExamples(scenarioOutline, examples, id).stream())
			.forEach(childID -> repository.attachChildNodeLast(id, childID));

		return Optional.of(id);
	}


	private UUID stepNode(Step step, UUID parent) {
		var node = new TestPlanNode(NodeType.STEP)
			.name(step.text())
			.language(feature.language())
			.keyword(step.keyword())
			.display("{keyword} {name}")
			.source(nodeLocation(step))
			.addProperties(propertiesFromComments(step))
			.addProperty(GHERKIN_TYPE,GHERKIN_TYPE_STEP);
		if (step.argument() instanceof DataTable dataTable) {
			node.dataTable(tableOf(dataTable));
		} else if (step.argument() instanceof DocString docString) {
			node.document(documentOf(docString));
		}
		return repository.persistNode(node);
	}


	/**
	 * Creates a step-aggregator node from the feature's {@code Background} section, if present.
	 *
	 * @param parent the parent node ID whose tags are inherited
	 * @param name   optional name override; if {@code null} or blank, the background's own
	 *               name is used
	 * @return the persisted background node ID, or {@code null} if the feature has no background
	 */
	public UUID createBackgroundStepsNode(UUID parent, String name) {
		if (background == null) {
			return null;
		}
		var node = new TestPlanNode(NodeType.STEP_AGGREGATOR)
			.name(notEmpty(name, background.name()))
			.language(feature.language())
			.keyword(background.keyword())
			.display("{keyword}")
			.description(background.description())
			.tags(tags(parent,background))
			.source(nodeLocation(background))
			.addProperties(propertiesFromComments(background, parent))
			.addProperty(GHERKIN_TYPE,GHERKIN_TYPE_BACKGROUND);
		var id = repository.persistNode(node);
		underlyingModels.put(id,background);
		background.children().forEach(step -> repository.attachChildNodeLast(id,stepNode(step, id)));
		return id;
	}


	/**
	 * Expands a scenario outline into individual scenario nodes by substituting placeholders
	 * with values from the given examples table. Each row in the examples table produces one
	 * test case node.
	 *
	 * @param scenarioOutline the scenario outline to expand
	 * @param examples        the examples table providing substitution values
	 * @param parent          the parent node ID for the generated scenarios
	 * @return list of persisted scenario node IDs
	 */
	public List<UUID> createScenariosFromExamples(
		ScenarioOutline scenarioOutline,
		Examples examples,
		UUID parent
	) {
		return indexMapped(substitutions(examples), (number, substitution) -> {
			var scenarioID = scenarioNode(scenarioOutline, number+1, parent);
			if (scenarioID.isEmpty()) {
				return scenarioID;
			}
			applySubstitution(scenarioID.orElseThrow(), substitution);
			return scenarioID;
		}).stream().filter(Optional::isPresent).map(Optional::get).toList();
	}


	private void applySubstitution(UUID nodeID, Function<String,String> substitution) {
		var node = repository.getNodeData(nodeID).orElseThrow();
		if (node.name() != null) {
			node.name(substitution.apply(node.name()));
		}
		if (node.description() != null) {
			node.description(substitution.apply(node.description()));
		}
		if (node.document() != null) {
			node.document(new Document(
				node.document().mimeType(),
				substitution.apply(node.document().content())
			));
		}
		if (node.dataTable() != null) {
			node.dataTable(new org.azertio.core.testplan.DataTable(
				node.dataTable().values().stream()
					.map(row -> row.stream().map(substitution).toList())
					.toList()
			));
		}
		repository.persistNode(node);
		repository.getNodeChildren(nodeID).forEach(childID -> applySubstitution(childID, substitution));
	}


	private String nodeLocation(Node node) {
		return "%s[%s,%s]".formatted(relativePath,node.location().line(),node.location().column());
	}


	private Map<String,String> propertiesFromComments(Commented node, UUID nodeID) {
		SortedMap<String,String> properties = new TreeMap<>(repository.getNodeProperties(nodeID));
		properties.putAll(propertiesFromComments(node));
		return properties;
	}


	private Map<String,String> propertiesFromComments(Commented node) {
		return node.comments().stream()
			.map(Comment::text)
			.map(propertyRegex::matcher)
			.filter(Matcher::find)
			.collect(Collectors.toMap(it -> it.group(1),it -> it.group(2)));
	}


	private Set<String> tags(Tagged node) {
		return node.tags().stream()
			.map(it -> it.name().substring(1))
			.filter(idTagPattern.asPredicate().negate())
			.collect(Collectors.toCollection(HashSet::new));
	}


	private Set<String> tags(UUID nodeID, Tagged node) {
		Set<String> result = new HashSet<>(repository.getNodeTags(nodeID));
		result.addAll(tags(node));
		return result;
	}


	private org.azertio.core.testplan.DataTable tableOf(DataTable dataTable) {
		return new org.azertio.core.testplan.DataTable(
				mapped(dataTable.rows(), row -> mapped(row.cells(), TableCell::value))
		);
	}


	private org.azertio.core.testplan.DataTable tableOf(Examples examples) {
		return new org.azertio.core.testplan.DataTable(concat(
				List.of(mapped(examples.tableHeader().cells(),TableCell::value)),
				mapped(examples.tableBody(), row -> mapped(row.cells(),TableCell::value))
		));
	}


	private Document documentOf(DocString docString) {
		return new Document(docString.contentType(), docString.content());
	}


	private List<Function<String,String>> substitutions(Examples examples) {
		var variables = mapped(
				examples.tableHeader().cells(),
				cell->"<"+cell.value()+">"
		);
		var values = mapped(
				examples.tableBody(),
				row -> mapped(row.cells(),TableCell::value)
		);
		return mapped(values, example ->
				indexMapped(example, (index,value) -> Map.entry(variables.get(index),value) )
						.stream()
						.map(this::toReplaceFunction)
						.reduce(Function::andThen)
						.orElseThrow()
		);
	}


	private Function<String,String> toReplaceFunction(Map.Entry<String,String> pair) {
		return ( input -> input.replace(pair.getKey(), pair.getValue()) );
	}


	private String idFromTags(Tagged tagged) {
		for (Tag tag : List.copyOf(tagged.tags())) {
			var matcher = this.idTagPattern.matcher(tag.name().substring(1));
			if (matcher.find()) {
				return matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0);
			}
		}
		return null;
	}


	/**
	 * Returns the mapping from plan node IDs to their underlying Gherkin model objects
	 * (e.g. {@link Feature}, {@link org.myjtools.gherkinparser.elements.ScenarioOutline}).
	 * This map is populated during {@link #createTestPlan()} and is used by
	 * {@link GherkinSuiteAssembler} for the redefining mechanism.
	 *
	 * @return an unmodifiable view would be safer, but currently returns the internal mutable map
	 */
	public Map<UUID,Object> underlyingModels() {
		return underlyingModels;
	}

	private static <T,U> List<U> indexMapped(List<T> list, BiFunction<Integer,T,U> mapper) {
		if (list == null) return List.of();
		List<U> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			result.add(mapper.apply(i,list.get(i)));
		}
		return List.copyOf(result);
	}

	private static <T,U> List<U> mapped(List<T> list, Function<T,U> mapper) {
		if (list == null) return List.of();
		return list.stream().map(mapper).toList();
	}

	private static <T> List<T> concat(List<T> list1, List<T> list2) {
		return Stream.concat(list1.stream(), list2.stream()).toList();
	}

	private static String notEmpty(String... options) {
		for (String option : options) {
			if (option != null && !option.isBlank()) {
				return option;
			}
		}
		return "";
	}
}
