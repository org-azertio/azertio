package org.azertio.core.expressions;

import org.azertio.core.Assertion;
import org.azertio.core.AssertionFactory;
import org.azertio.core.AssertionPattern;
import java.util.Locale;

/**
 * Fragment matcher for assertion patterns in expressions.
 *
 * <p>This matcher handles expression assertions defined with {@code {{assertion-name}}}
 * syntax. It uses an {@link AssertionFactory} to match localized assertion patterns
 * and create {@link Assertion} instances for validation.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // For expression: "the value {{number-assertion}}"
 * // Matches: "the value is greater than 10"
 * // Returns an Assertion that tests if values are > 10
 * }</pre>
 *
 * @param <T> the type of values the assertions handle
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see FragmentMatcher
 * @see AssertionFactory
 * @see Assertion
 */
public class AssertionFactoryFragmentMatcher<T> implements FragmentMatcher {

	private final AssertionFactory<T> assertionFactory;

	/**
	 * Creates an assertion factory fragment matcher.
	 *
	 * @param assertionFactory the factory providing assertion patterns
	 */
	public AssertionFactoryFragmentMatcher(AssertionFactory<T> assertionFactory) {
		this.assertionFactory = assertionFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MatchResult matches(String input, Locale locale) {
		for (AssertionPattern<T> pattern : assertionFactory.patterns(locale)) {
			var matcher = pattern.matcher(input);
			if (matcher.find()) {
				Assertion assertion = assertionFactory.assertion(pattern,input);
				return new MatchResult(true, matcher.end(), assertion);
			}
		}
		return new MatchResult(false);
	}

	@Override
	public String toString() {
		return "AssertionFactory["+assertionFactory.name()+"]";
	}
}
