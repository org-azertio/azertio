package org.azertio.core.expressions;

import org.azertio.core.DataType;

/**
 * Represents a literal argument value extracted from expression matching.
 *
 * <p>A literal value contains the raw text matched from the input, which can be
 * converted to a typed object using the associated {@link DataType}.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // For input "the count is 42" matching expression "the count is {count:number}"
 * LiteralValue value = (LiteralValue) match.argument("count");
 * String literal = value.literal();  // "42"
 * Object parsed = value.value();     // Integer 42
 * }</pre>
 *
 * @param name    the argument name
 * @param literal the literal text value from the input
 * @param type    the data type for parsing
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ArgumentValue
 * @see VariableValue
 */
public record LiteralValue(String name, String literal, DataType type) implements ArgumentValue {

	/**
	 * Parses and returns the typed value.
	 *
	 * @return the parsed value according to the data type
	 */
	public Object value() {
		return type.parse(literal);
	}

}
