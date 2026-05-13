package org.azertio.core.expressions;

import org.azertio.core.Assertion;

import java.util.Locale;

/**
 * Interface for matching fragments of an expression against input text.
 *
 * <p>Fragment matchers are the building blocks of {@link ExpressionMatcher}.
 * Each implementation handles a specific type of expression fragment:</p>
 *
 * <ul>
 *   <li>{@link PatternFragmentMatcher} - Matches using regex patterns</li>
 *   <li>{@link ArgumentFragmentMatcher} - Matches typed arguments</li>
 *   <li>{@link AssertionFactoryFragmentMatcher} - Matches assertion patterns</li>
 * </ul>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ExpressionMatcher
 */
public interface FragmentMatcher {

	/**
	 * Result of a fragment matching operation.
	 *
	 * @param startMatched whether the fragment matched at the start of the input
	 * @param consumed     number of characters consumed from the input
	 * @param argument     extracted argument value, if any
	 * @param assertion    extracted assertion, if any
	 */
	record MatchResult(
			boolean startMatched,
			int consumed,
			ArgumentValue argument,
			Assertion assertion
	) {
		public MatchResult(boolean startMatched) {
			this(startMatched, 0, null, null);
		}

		public MatchResult(boolean startMatched, int consumed) {
			this(startMatched, consumed, null, null);
		}

		public MatchResult(boolean startMatched, int consumed, ArgumentValue argumentValue) {
			this(startMatched, consumed, argumentValue, null);
		}

		public MatchResult(boolean startMatched, int consumed, Assertion assertion) {
			this(startMatched, consumed, null, assertion);
		}


	}

	/**
	 * Attempts to match this fragment against the input string.
	 *
	 * @param input  the input string to match against
	 * @param locale the locale for localized matching (e.g., assertions)
	 * @return the match result indicating success/failure and consumed characters
	 */
	MatchResult matches(String input, Locale locale);

}
