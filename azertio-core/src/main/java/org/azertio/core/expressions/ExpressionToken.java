package org.azertio.core.expressions;

/**
 * Represents a token produced by the {@link ExpressionTokenizer}.
 *
 * <p>A token captures a syntactic element from the expression string, including its
 * type, optional value (for TEXT tokens), and position information for error reporting.</p>
 *
 * @param type  the type of this token
 * @param value the text value (only for TEXT tokens, null for symbol tokens)
 * @param start the start position in the original expression
 * @param end   the end position in the original expression
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ExpressionTokenType
 * @see ExpressionTokenizer
 */
public record ExpressionToken(ExpressionTokenType type, String value, int start, int end) {

	/**
	 * Creates a symbol token without a value.
	 *
	 * @param type  the token type
	 * @param start the start position
	 * @param end   the end position
	 */
	public ExpressionToken(ExpressionTokenType type, int start, int end) {
		this(type, null, start, end);
	}

	/**
	 * Creates a symbol token at a single position.
	 *
	 * @param type     the token type
	 * @param position the position in the expression
	 */
	public ExpressionToken(ExpressionTokenType type, int position) {
		this(type, null, position, position);
	}


	@Override
	public String toString() {
		if (value != null) {
			return "Token[%s '%s' %s-%s]".formatted(type,value,start,end);
		} else {
			return "Token[%s %s-%s]".formatted(type,start,end);
		}
	}

	/**
	 * Checks if the token value starts with a blank space.
	 *
	 * @return {@code true} if the value starts with a space
	 */
	public boolean startsWithBlank() {
		return value.startsWith(" ");
	}

	/**
	 * Checks if the token value ends with a blank space.
	 *
	 * @return {@code true} if the value ends with a space
	 */
	public boolean endsWithBlank() {
		return value.endsWith(" ");
	}

	/**
	 * Checks if the token value contains only a single word (no spaces).
	 *
	 * @return {@code true} if the value is a single word
	 */
	public boolean isSingleWord() {
		return value.strip().chars().filter(c -> c == ' ').count() == 0L;
	}

	/**
	 * Extracts the first word from the token value.
	 *
	 * @return the first word
	 */
	public String firstWord() {
		return value.strip().split(" ")[0];
	}

	/**
	 * Creates a new token containing only the first word.
	 *
	 * @return a new token with the first word
	 */
	public ExpressionToken firstWordToken() {
		String firstWord = firstWord();
		return new ExpressionToken(type, firstWord, start, start+firstWord.length());
	}

	/**
	 * Extracts the last word from the token value.
	 *
	 * @return the last word
	 */
	public String lastWord() {
		var words = value.strip().split(" ");
		return words[words.length-1];
	}

	/**
	 * Creates a new token with the specified number of leading characters removed.
	 *
	 * @param length the number of characters to remove from the start
	 * @return a new token with leading characters removed
	 */
	public ExpressionToken removeLeadingChars(int length) {
		return new ExpressionToken(type, value.substring(length), start+length, end);
	}

	/**
	 * Creates a new token with the specified number of trailing characters removed.
	 *
	 * @param length the number of characters to remove from the end
	 * @return a new token with trailing characters removed
	 */
	public ExpressionToken removeTrailingChars(int length) {
		return new ExpressionToken(type, value.substring(0,value.length()-length), start, end-length);
	}

}
