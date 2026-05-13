package org.azertio.core.assertions;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.azertio.core.Assertion;


/**
 * Adapter that wraps a Hamcrest {@link Matcher} to implement the Azertio {@link Assertion} interface.
 *
 * <p>This class acts as a bridge between the Hamcrest matcher framework and the Azertio
 * assertion system, allowing reuse of the extensive library of matchers available in
 * Hamcrest while maintaining compatibility with the Azertio API.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create an assertion that verifies equality
 * Assertion assertion = new AssertionAdapter("equals-check", Matchers.equalTo(42));
 *
 * // Evaluate the assertion
 * boolean result = assertion.test(42); // true
 * boolean failed = assertion.test(40); // false
 *
 * // Get failure description
 * String failureMessage = assertion.describeFailure(40); // "was <40>"
 * }</pre>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see Assertion
 * @see Matcher
 */
public class AssertionAdapter implements Assertion {

	private final Matcher<?> matcher;


	/**
	 * Creates a new assertion adapter.
	 *
	 * @param matcher the Hamcrest matcher that will perform the validation
	 */
	public AssertionAdapter(Matcher<?> matcher) {
		this.matcher = matcher;
	}

	/**
	 * Tests whether the actual value matches the expected condition.
	 *
	 * @param actualValue the value to test
	 * @return {@code true} if the value matches, {@code false} otherwise
	 */
	@Override
	public boolean test(Object actualValue) {
		return matcher.matches(actualValue);
	}

	/**
	 * Describes why the assertion failed for the given value.
	 *
	 * @param actualValue the value that failed the assertion
	 * @return a human-readable description of the mismatch
	 */
	@Override
	public String describeFailure(Object actualValue) {
		Description description = new StringDescription();
		matcher.describeMismatch(actualValue, description);
		return description.toString();
	}


}
