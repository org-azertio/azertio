package org.azertio.core.expressions;


import org.azertio.core.DataType;
import org.azertio.core.util.Patterns;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Fragment matcher for typed arguments in expressions.
 *
 * <p>This matcher handles expression arguments defined with {@code {type}} or
 * {@code {name:type}} syntax. It can match:</p>
 * <ul>
 *   <li>Literal values according to the data type pattern (e.g., numbers, dates)</li>
 *   <li>Variable references in the form {@code ${variableName}}</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // For expression: "the count is {count:number}"
 * // Matches: "the count is 42" -> LiteralValue("count", "42", NUMBER)
 * // Matches: "the count is ${myVar}" -> VariableValue("count", "myVar", NUMBER)
 * }</pre>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see FragmentMatcher
 * @see LiteralValue
 * @see VariableValue
 */
public class ArgumentFragmentMatcher implements FragmentMatcher {

	private static final Pattern variable = Patterns.of("\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

	private final DataType dataType;
	private final String name;

	/**
	 * Creates an argument matcher using the data type name as the argument name.
	 *
	 * @param dataType the data type for this argument
	 */
	public ArgumentFragmentMatcher(DataType dataType) {
		this(dataType.name(), dataType);
	}

	/**
	 * Creates an argument matcher with an explicit name.
	 *
	 * @param name     the argument name
	 * @param dataType the data type for this argument
	 */
	public ArgumentFragmentMatcher(String name, DataType dataType) {
		this.dataType = dataType;
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MatchResult matches(String input, Locale locale) {
		var dataTypePattern = dataType.pattern().matcher(input);
		if (dataTypePattern.lookingAt()) {
			return new MatchResult(
				true,
				dataTypePattern.end(),
				new LiteralValue(name, dataTypePattern.group(), dataType)
			);
		} else {
			var variableMatcher = variable.matcher(input);
			if (variableMatcher.lookingAt()) {
				return new MatchResult(
					true,
					variableMatcher.end(),
					new VariableValue(name, variableMatcher.group(1), dataType)
				);
			}
			return new MatchResult(false);
		}
	}


	@Override
	public String toString() {
		return "Argument["+name+":"+dataType.name()+"]";
	}

	/**
	 * Returns the data type of this argument.
	 *
	 * @return the data type
	 */
	public DataType type() {
		return dataType;
	}

	/**
	 * Returns the name of this argument.
	 *
	 * @return the argument name
	 */
	public String name() {
		return name;
	}
}
