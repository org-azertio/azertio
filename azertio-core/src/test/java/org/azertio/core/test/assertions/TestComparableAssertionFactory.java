package org.azertio.core.test.assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.azertio.core.Assertion;
import org.azertio.core.AssertionFactories;
import org.azertio.core.AssertionFactory;
import org.azertio.core.AssertionPattern;
import org.azertio.core.assertions.ComparableAssertionFactory;
import org.azertio.core.assertions.CoreAssertionFactories;
import java.util.List;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ComparableAssertionFactory}.
 */
class TestComparableAssertionFactory {

	static AssertionFactories factories = AssertionFactories.of(CoreAssertionFactories.ALL);

	private AssertionFactory<?> numberFactory;

	@BeforeEach
	void setUp() {
		numberFactory = factories.byName("integer-assertion");
	}

	@Test
	void name_shouldReturnFactoryName() {
		assertThat(numberFactory.name()).isEqualTo("integer-assertion");
	}

	@Test
	void patterns_shouldReturnPatternsForLocale() {
		List<? extends AssertionPattern<?>> patterns = numberFactory.patterns(Locale.ENGLISH);

		assertThat(patterns).isNotEmpty();
	}

	@Test
	void assertion_equalsPattern_shouldMatchEqualValues() {
		Assertion assertion = findAndCreateAssertion(
			ComparableAssertionFactory.ASSERTION_NUMBER_EQUALS, "42");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(42)).isTrue();
		assertThat(assertion.test(40)).isFalse();
	}

	@Test
	void assertion_greaterThanPattern_shouldMatchGreaterValues() {
		Assertion assertion = findAndCreateAssertion(
			ComparableAssertionFactory.ASSERTION_NUMBER_GREATER, "10");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(15)).isTrue();
		assertThat(assertion.test(10)).isFalse();
		assertThat(assertion.test(5)).isFalse();
	}

	@Test
	void assertion_lessThanPattern_shouldMatchLesserValues() {
		Assertion assertion = findAndCreateAssertion(
			ComparableAssertionFactory.ASSERTION_NUMBER_LESS, "10");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(5)).isTrue();
		assertThat(assertion.test(10)).isFalse();
		assertThat(assertion.test(15)).isFalse();
	}

	@Test
	void assertion_greaterOrEqualPattern_shouldMatchGreaterOrEqualValues() {
		Assertion assertion = findAndCreateAssertion(
			ComparableAssertionFactory.ASSERTION_NUMBER_GREATER_EQUALS, "10");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(15)).isTrue();
		assertThat(assertion.test(10)).isTrue();
		assertThat(assertion.test(5)).isFalse();
	}

	@Test
	void assertion_lessOrEqualPattern_shouldMatchLessOrEqualValues() {
		Assertion assertion = findAndCreateAssertion(
			ComparableAssertionFactory.ASSERTION_NUMBER_LESS_EQUALS, "10");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(5)).isTrue();
		assertThat(assertion.test(10)).isTrue();
		assertThat(assertion.test(15)).isFalse();
	}

	@Test
	void assertion_notEqualsPattern_shouldMatchDifferentValues() {
		Assertion assertion = findAndCreateAssertion(
			ComparableAssertionFactory.ASSERTION_NUMBER_NOT_EQUALS, "42");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(40)).isTrue();
		assertThat(assertion.test(42)).isFalse();
	}

	@Test
	void assertion_nullPattern_shouldMatchNullValue() {
		Assertion assertion = findAndCreateAssertion(
			ComparableAssertionFactory.ASSERTION_GENERIC_NULL, null);

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(null)).isTrue();
		assertThat(assertion.test(42)).isFalse();
	}

	@Test
	void assertion_notNullPattern_shouldMatchNonNullValue() {
		Assertion assertion = findAndCreateAssertion(
			ComparableAssertionFactory.ASSERTION_GENERIC_NOT_NULL, null);

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(42)).isTrue();
		assertThat(assertion.test(null)).isFalse();
	}

	@SuppressWarnings("unchecked")
	private Assertion findAndCreateAssertion(String patternKey, String value) {
		List<AssertionPattern<Object>> patterns =
			(List<AssertionPattern<Object>>) (List<?>) numberFactory.patterns(Locale.ENGLISH);

		for (AssertionPattern<Object> pattern : patterns) {
			if (pattern.key().equals(patternKey)) {
				// Create a test input that matches the pattern
				String input = createInputForPattern(patternKey, value);
				return ((AssertionFactory<Object>) numberFactory).assertion(pattern, input);
			}
		}
		return null;
	}

	private String createInputForPattern(String patternKey, String value) {
		// These inputs match the patterns defined in AssertionFactoryProvider_en.properties
		return switch (patternKey) {
			case ComparableAssertionFactory.ASSERTION_NUMBER_EQUALS -> "is equal to " + value;
			case ComparableAssertionFactory.ASSERTION_NUMBER_GREATER -> "is greater than " + value;
			case ComparableAssertionFactory.ASSERTION_NUMBER_LESS -> "is less than " + value;
			case ComparableAssertionFactory.ASSERTION_NUMBER_GREATER_EQUALS -> "is greater than or equal to " + value;
			case ComparableAssertionFactory.ASSERTION_NUMBER_LESS_EQUALS -> "is less than or equal to " + value;
			case ComparableAssertionFactory.ASSERTION_NUMBER_NOT_EQUALS -> "is not equal to " + value;
			case ComparableAssertionFactory.ASSERTION_GENERIC_NULL -> "is null";
			case ComparableAssertionFactory.ASSERTION_GENERIC_NOT_NULL -> "is not null";
			default -> value != null ? value : "";
		};
	}
}