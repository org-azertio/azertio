package org.azertio.core.expressions;

import org.azertio.core.AzertioException;

import java.io.Serial;

/**
 * Exception thrown when expression parsing or matching fails.
 *
 * <p>This exception is thrown by:</p>
 * <ul>
 *   <li>{@link ExpressionTokenizer} - For invalid escape sequences</li>
 *   <li>{@link ExpressionASTBuilder} - For malformed expression syntax</li>
 *   <li>{@link ExpressionMatcherBuilder} - For invalid expression structure</li>
 * </ul>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see AzertioException
 */
public class ExpressionException extends AzertioException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Creates an exception with position information.
	 *
	 * @param text     the expression text
	 * @param position the error position in the text
	 * @param message  the error message
	 */
	public ExpressionException(String text, int position, String message) {
		super("Error in expression {} at position {}: {}. {}", position, text, message);
	}

	/**
	 * Creates an exception with a formatted message.
	 *
	 * @param message the message pattern
	 * @param args    the message arguments
	 */
	public ExpressionException(String message, Object... args) {
		super(message, args);
	}
}
