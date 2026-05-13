package org.azertio.core.expressions;

import org.azertio.core.DataType;

/**
 * Represents a variable reference extracted from expression matching.
 *
 * <p>A variable value is matched when the input contains a variable reference
 * in the form {@code ${variableName}}. The actual value must be resolved
 * at runtime from an execution context.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // For input "the count is ${myCount}" matching expression "the count is {count:number}"
 * VariableValue value = (VariableValue) match.argument("count");
 * String varName = value.variable();  // "myCount"
 * // Actual value must be resolved from context
 * }</pre>
 *
 * @param name     the argument name from the expression
 * @param variable the variable name from the input (without ${})
 * @param type     the expected data type
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ArgumentValue
 * @see LiteralValue
 */
public record VariableValue(String name, String variable, DataType type) implements ArgumentValue {
}
