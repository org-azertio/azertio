package org.azertio.core.expressions;

import org.azertio.core.DataType;

/**
 * Sealed interface for argument values extracted from expression matching.
 *
 * <p>Argument values can be either:</p>
 * <ul>
 *   <li>{@link LiteralValue} - A literal value parsed from the input text</li>
 *   <li>{@link VariableValue} - A variable reference like {@code ${varName}}</li>
 * </ul>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see LiteralValue
 * @see VariableValue
 * @see Match
 */
public sealed interface ArgumentValue permits VariableValue, LiteralValue {

	/**
	 * Returns the data type of this argument.
	 *
	 * @return the data type
	 */
	DataType type();

	/**
	 * Returns the name of this argument.
	 *
	 * @return the argument name
	 */
	String name();

}
