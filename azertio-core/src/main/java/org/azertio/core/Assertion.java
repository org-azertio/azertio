package org.azertio.core;

/**
 * The {@code Assertion} interface defines a contract for creating assertions on a given value.
 * Assertions include a test method for evaluating the condition and methods for providing a
 * description and describing a failure when the assertion is not satisfied.
 *

 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
public interface Assertion {

	/**
	 * Static utility method for asserting a condition on a given value.
	 * Throws an {@code AssertionError} if the assertion fails.
	 *
	 * @param actualValue The value to test against the assertion.
	 * @param assertion   The assertion condition to apply.
	 */
	static void assertThat(Object actualValue, Assertion assertion) {
		if (!assertion.test(actualValue)) {
			throw new AssertionError(assertion.describeFailure(actualValue));
		}
	}

	/**
	 * Tests the assertion condition against the provided value.
	 *
	 * @param actualValue The value to test the assertion against.
	 * @return {@code true} if the assertion is satisfied, {@code false} otherwise.
	 */
	boolean test(Object actualValue);



	/**
	 * Describes the failure when the assertion is not satisfied.
	 *
	 * @param actualValue The value that failed the assertion.
	 * @return A string describing the failure of the assertion.
	 */
	String describeFailure(Object actualValue);


}