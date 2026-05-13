package org.azertio.plugins.gherkin;

/**
 * Property keys and values used to annotate {@link org.azertio.core.testplan.TestPlanNode}
 * elements with their originating Gherkin type. These constants are stored as node properties
 * so that downstream processors can identify the Gherkin origin of each plan node.
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class GherkinConstants {

	private GherkinConstants() { }

	/** Property key indicating the Gherkin element type of a plan node. */
	public static final String GHERKIN_TYPE = "gherkinType";

	/** Property value for {@link #GHERKIN_TYPE} representing a Gherkin {@code Feature}. */
	public static final String GHERKIN_TYPE_FEATURE = "feature";

	/** Property value for {@link #GHERKIN_TYPE} representing a Gherkin {@code Scenario}. */
	public static final String GHERKIN_TYPE_SCENARIO = "scenario";

	/** Property value for {@link #GHERKIN_TYPE} representing a Gherkin {@code Scenario Outline}. */
	public static final String GHERKIN_TYPE_SCENARIO_OUTLINE = "scenarioOutline";

	/** Property value for {@link #GHERKIN_TYPE} representing a Gherkin {@code Background}. */
	public static final String GHERKIN_TYPE_BACKGROUND = "background";

	/** Property value for {@link #GHERKIN_TYPE} representing a Gherkin {@code Step}. */
	public static final String GHERKIN_TYPE_STEP = "step";

}
