package org.azertio.core.expressions;

import org.azertio.core.Assertion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of matching an input string against an {@link ExpressionMatcher}.
 *
 * <p>A Match contains:</p>
 * <ul>
 *   <li>Whether the input matched the expression pattern</li>
 *   <li>Extracted argument values (from {@code {arg}} patterns)</li>
 *   <li>Created assertions (from {@code {{assertion}}} patterns)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Match match = matcher.matches("the user John has 5 items", Locale.ENGLISH);
 * if (match.matched()) {
 *     ArgumentValue name = match.argument("name");
 *     Assertion countAssertion = match.assertion("number-assertion");
 * }
 * }</pre>
 *
 * @param arguments   list of argument names to their extracted values
 * @param assertion  assertion
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ExpressionMatcher
 * @see ArgumentValue
 * @see Assertion
 */
public record Match (
	List<ArgumentValue> arguments,
	Assertion assertion
) {




	public Map<String, Object> interpolateArguments(Map<String,Object> variableValues) {
		Map<String,Object> args = new HashMap<>();
		for (var argument : arguments) {
			if (argument instanceof LiteralValue literalValue) {
				args.put(literalValue.name(),literalValue.value());
			} else if (argument instanceof VariableValue variableValue) {
				args.put(variableValue.name(), variableValues.get(variableValue.variable()));
			}
		}
		return args;
	}

}
