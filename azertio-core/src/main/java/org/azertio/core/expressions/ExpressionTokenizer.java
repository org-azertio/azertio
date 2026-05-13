package org.azertio.core.expressions;

import java.util.*;

/**
 * Lexical analyzer that converts expression strings into a sequence of tokens.
 *
 * <p>The tokenizer recognizes the following special characters:</p>
 * <ul>
 *   <li>{@code ^} - Negation</li>
 *   <li>{@code ( )} - Optional groups</li>
 *   <li>{@code [ ]} - Required groups</li>
 *   <li>{@code { }} - Arguments (single) or Assertions (double {@code {{ }}})</li>
 *   <li>{@code |} - Choice separator</li>
 *   <li>{@code *} - Wildcard</li>
 *   <li>{@code \} - Escape character</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ExpressionTokenizer tokenizer = new ExpressionTokenizer("the value is {number}");
 * List<ExpressionToken> tokens = tokenizer.tokens();
 * // Produces: [TEXT "the value is ", START_ARGUMENT, TEXT "number", END_ARGUMENT]
 * }</pre>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ExpressionToken
 * @see ExpressionTokenType
 */
public class ExpressionTokenizer {

	private static final char NEGATION_SYMBOL = '^';
	private static final char START_OPTIONAL_SYMBOL = '(';
	private static final char CHOICE_SYMBOL = '|';
	private static final char START_GROUP_SYMBOL = '[';
	private static final char WILDCARD_SYMBOL = '*';
	private static final char START_ARGUMENT_SYMBOL = '{';
	private static final char END_GROUP_SYMBOL = ']';
	private static final char END_OPTIONAL_SYMBOL = ')';
	private static final char END_ARGUMENT_SYMBOL = '}';
	private static final char ESCAPE_SYMBOL = '\\';

	public static final char[] symbols = new char[] {
		WILDCARD_SYMBOL,
		ESCAPE_SYMBOL,
		START_OPTIONAL_SYMBOL,
		END_OPTIONAL_SYMBOL,
		START_ARGUMENT_SYMBOL,
		END_ARGUMENT_SYMBOL,
		NEGATION_SYMBOL,
		START_GROUP_SYMBOL,
		END_GROUP_SYMBOL,
		CHOICE_SYMBOL
	};

	static {
		Arrays.sort(symbols);
	}

	private static boolean isSymbol(char c) {
		return Arrays.binarySearch(symbols,c) >= 0;
	}


	static final Map<Character, ExpressionTokenType> tokenTypeBySymbol = Map.ofEntries(
		Map.entry(NEGATION_SYMBOL, ExpressionTokenType.NEGATION),
		Map.entry(START_OPTIONAL_SYMBOL, ExpressionTokenType.START_OPTIONAL),
		Map.entry(END_OPTIONAL_SYMBOL, ExpressionTokenType.END_OPTIONAL),
		Map.entry(WILDCARD_SYMBOL, ExpressionTokenType.WILDCARD),
		Map.entry(START_ARGUMENT_SYMBOL, ExpressionTokenType.START_ARGUMENT),
		Map.entry(END_ARGUMENT_SYMBOL, ExpressionTokenType.END_ARGUMENT),
		Map.entry(START_GROUP_SYMBOL, ExpressionTokenType.START_GROUP),
		Map.entry(END_GROUP_SYMBOL, ExpressionTokenType.END_GROUP),
		Map.entry(CHOICE_SYMBOL, ExpressionTokenType.CHOICE_SEPARATOR)
	);


	private final String text;
	private final StringBuilder buffer;
	private boolean escaped = false;
	private int position = 0;
	private int tokenStart = 0;
	private List<ExpressionToken> tokens = null;


	public ExpressionTokenizer(String text) {
		this.text = text.trim();
		this.buffer = new StringBuilder(text.length());
	}


	public List<ExpressionToken> tokens() {
		if (tokens == null) {
			tokens = new ArrayList<>();
			while (hasNext()) {
				next();
			}
			dumpBuffer();
		}
		return tokens;
	 }


	private void next() {

		char current = text.charAt(position);
		char next = (position == text.length() - 1 ? 0 : text.charAt(position + 1));

		if (escaped) {
			if (isSymbol(current)) {
				buffer.append(current);
				escaped = false;
				position++;
				return;
			} else {
				abort("unexpected escaped character "+ current);
			}
		}

		if (current == ESCAPE_SYMBOL) {
			escaped = true;
			position++;
			return;
		}

		if (Character.isWhitespace(current) && Character.isWhitespace(next)) {
			position++;
			return;
		}

		if (isSymbol(current)) {
			dumpBuffer();
			if (current == START_ARGUMENT_SYMBOL) {
				if (next == START_ARGUMENT_SYMBOL) {
					addToken(ExpressionTokenType.START_ASSERTION);
					position++;
				} else {
					addToken(ExpressionTokenType.START_ARGUMENT);
				}
			} else if (current == END_ARGUMENT_SYMBOL) {
				if (next == END_ARGUMENT_SYMBOL) {
					addToken(ExpressionTokenType.END_ASSERTION);
					position++;
				} else {
					addToken(ExpressionTokenType.END_ARGUMENT);
				}
			} else {
				addToken(tokenTypeBySymbol.get(current));
			}
		} else {
			buffer.append(current);
		}

		position++;

	}


	private void addToken(ExpressionTokenType type) {
		tokens.add(new ExpressionToken(type,tokenStart));
		tokenStart++;
	}


	private boolean hasNext() {
		return position < text.length();
	}


	private void dumpBuffer() {
		String value = buffer.toString();
		buffer.delete(0, buffer.length());
		if (!value.isBlank()) {
			tokens.add(new ExpressionToken(ExpressionTokenType.TEXT, value, tokenStart, position));
			tokenStart = position + 1;
		}
	}


	private void abort(String message) {
		throw new ExpressionException(text,position,message);
	}


}
