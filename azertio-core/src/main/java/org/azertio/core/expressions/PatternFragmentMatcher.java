package org.azertio.core.expressions;

import org.azertio.core.util.Patterns;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Fragment matcher that uses regular expressions to match input text.
 *
 * <p>This matcher is used for literal text, optional groups, choices, negations,
 * and wildcards - essentially any part of the expression that can be represented
 * as a regex pattern.</p>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see FragmentMatcher
 */
public class PatternFragmentMatcher implements FragmentMatcher {

	private final Pattern pattern;
	private final String literal;

	/**
	 * Creates a new pattern fragment matcher.
	 *
	 * @param regex   the regex pattern to match
	 * @param literal the literal representation for display purposes
	 */
	public PatternFragmentMatcher(String regex, String literal) {
		this.pattern = Patterns.ofIgnoreCase(regex);
		this.literal = literal;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MatchResult matches(String input, Locale locale) {
		var matcher = pattern.matcher(input);
		if (matcher.lookingAt()) {
			return new MatchResult(true, matcher.end());
		} else {
			return new MatchResult(false, 0);
		}
	}


	/**
	 * Returns the compiled regex pattern.
	 *
	 * @return the pattern
	 */
	public Pattern pattern() {
		return pattern;
	}

	/**
	 * Returns the literal representation of this pattern.
	 *
	 * @return the literal string
	 */
	public String literal() {
		return literal;
	}

	@Override
	public String toString() {
		return "Pattern["+literal+"]";
	}
}
