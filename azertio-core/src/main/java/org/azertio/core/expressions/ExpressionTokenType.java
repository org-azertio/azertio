package org.azertio.core.expressions;

/**
 * Enumeration of token types produced by the {@link ExpressionTokenizer}.
 *
 * <p>Each token type represents a distinct syntactic element in the expression language:</p>
 *
 * <ul>
 *   <li>{@link #TEXT} - Literal text content</li>
 *   <li>{@link #NEGATION} - The {@code ^} symbol for negation</li>
 *   <li>{@link #START_OPTIONAL}/{@link #END_OPTIONAL} - Parentheses {@code ( )}</li>
 *   <li>{@link #START_GROUP}/{@link #END_GROUP} - Brackets {@code [ ]}</li>
 *   <li>{@link #START_ARGUMENT}/{@link #END_ARGUMENT} - Single braces {@code { }}</li>
 *   <li>{@link #START_ASSERTION}/{@link #END_ASSERTION} - Double braces {@code {{ }}}</li>
 *   <li>{@link #CHOICE_SEPARATOR} - The pipe {@code |} symbol</li>
 *   <li>{@link #WILDCARD} - The asterisk {@code *} symbol</li>
 * </ul>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ExpressionToken
 * @see ExpressionTokenizer
 */
public enum ExpressionTokenType {
	/** Negation marker {@code ^} - matches anything except the following word/phrase. */
	NEGATION,
	/** Start of optional group {@code (}. */
	START_OPTIONAL,
	/** End of optional group {@code )}. */
	END_OPTIONAL,
	/** Choice separator {@code |} - separates alternatives. */
	CHOICE_SEPARATOR,
	/** Start of required group {@code [}. */
	START_GROUP,
	/** End of required group {@code ]}. */
	END_GROUP,
	/** Start of argument {@code {} - begins a typed parameter. */
	START_ARGUMENT,
	/** End of argument {@code }} - ends a typed parameter. */
	END_ARGUMENT,
	/** Start of assertion {@code {{} - begins an assertion reference. */
	START_ASSERTION,
	/** End of assertion {@code }}} - ends an assertion reference. */
	END_ASSERTION,
	/** Literal text content. */
	TEXT,
	/** Wildcard {@code *} - matches any text. */
	WILDCARD
}
