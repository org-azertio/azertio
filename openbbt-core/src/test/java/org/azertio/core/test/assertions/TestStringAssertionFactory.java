package org.azertio.core.test.assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.azertio.core.Assertion;
import org.azertio.core.AssertionFactories;
import org.azertio.core.AssertionFactory;
import org.azertio.core.AssertionPattern;
import org.azertio.core.assertions.CoreAssertionFactories;
import org.azertio.core.assertions.StringAssertionFactory;
import java.util.List;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringAssertionFactory}.
 */
class TestStringAssertionFactory {

	static AssertionFactories factories = AssertionFactories.of(CoreAssertionFactories.ALL);

	private AssertionFactory<?> textFactory;

	@BeforeEach
	void setUp() {
		textFactory = factories.byName("text-assertion");
	}

	@Test
	void name_shouldReturnFactoryName() {
		assertThat(textFactory.name()).isEqualTo("text-assertion");
	}

	@Test
	void patterns_shouldReturnPatternsForLocale() {
		List<? extends AssertionPattern<?>> patterns = textFactory.patterns(Locale.ENGLISH);

		assertThat(patterns).isNotEmpty();
	}

	@Test
	void assertion_equalsPattern_shouldMatchEqualStrings() {
		Assertion assertion = findAndCreateAssertion(
			StringAssertionFactory.ASSERTION_STRING_EQUALS, "\"hello\"");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test("hello")).isTrue();
		assertThat(assertion.test("world")).isFalse();
	}

	@Test
	void assertion_notEqualsPattern_shouldMatchDifferentStrings() {
		Assertion assertion = findAndCreateAssertion(
			StringAssertionFactory.ASSERTION_STRING_NOT_EQUALS, "\"hello\"");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test("world")).isTrue();
		assertThat(assertion.test("hello")).isFalse();
	}

	@Test
	void assertion_containsPattern_shouldMatchContainingStrings() {
		Assertion assertion = findAndCreateAssertion(
			StringAssertionFactory.ASSERTION_STRING_CONTAINS, "\"ell\"");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test("hello")).isTrue();
		assertThat(assertion.test("world")).isFalse();
	}

	@Test
	void assertion_startsWithPattern_shouldMatchPrefixes() {
		Assertion assertion = findAndCreateAssertion(
			StringAssertionFactory.ASSERTION_STRING_STARTS_WITH, "\"hel\"");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test("hello")).isTrue();
		assertThat(assertion.test("world")).isFalse();
	}

	@Test
	void assertion_endsWithPattern_shouldMatchSuffixes() {
		Assertion assertion = findAndCreateAssertion(
			StringAssertionFactory.ASSERTION_STRING_ENDS_WITH, "\"llo\"");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test("hello")).isTrue();
		assertThat(assertion.test("world")).isFalse();
	}

	@Test
	void assertion_equalsIgnoreCasePattern_shouldMatchCaseInsensitive() {
		Assertion assertion = findAndCreateAssertion(
			StringAssertionFactory.ASSERTION_STRING_EQUALS_IGNORE_CASE, "\"HELLO\"");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test("hello")).isTrue();
		assertThat(assertion.test("HELLO")).isTrue();
		assertThat(assertion.test("HeLLo")).isTrue();
		assertThat(assertion.test("world")).isFalse();
	}

	@Test
	void assertion_containsIgnoreCasePattern_shouldMatchCaseInsensitive() {
		Assertion assertion = findAndCreateAssertion(
			StringAssertionFactory.ASSERTION_STRING_CONTAINS_IGNORE_CASE, "\"ELL\"");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test("hello")).isTrue();
		assertThat(assertion.test("HELLO")).isTrue();
		assertThat(assertion.test("world")).isFalse();
	}

	@Test
	void assertion_notContainsPattern_shouldMatchNonContainingStrings() {
		Assertion assertion = findAndCreateAssertion(
			StringAssertionFactory.ASSERTION_STRING_NOT_CONTAINS, "\"xyz\"");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test("hello")).isTrue();
		assertThat(assertion.test("xyz world")).isFalse();
	}

	@Test
	void assertion_nullPattern_shouldMatchNullValue() {
		Assertion assertion = findAndCreateAssertion(
			StringAssertionFactory.ASSERTION_GENERIC_NULL, null);

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(null)).isTrue();
		assertThat(assertion.test("hello")).isFalse();
	}

	@Test
	void assertion_notNullPattern_shouldMatchNonNullValue() {
		Assertion assertion = findAndCreateAssertion(
			StringAssertionFactory.ASSERTION_GENERIC_NOT_NULL, null);

		assertThat(assertion).isNotNull();
		assertThat(assertion.test("hello")).isTrue();
		assertThat(assertion.test(null)).isFalse();
	}

	@SuppressWarnings("unchecked")
	private Assertion findAndCreateAssertion(String patternKey, String value) {
		List<AssertionPattern<Object>> patterns =
			(List<AssertionPattern<Object>>) (List<?>) textFactory.patterns(Locale.ENGLISH);

		for (AssertionPattern<Object> pattern : patterns) {
			if (pattern.key().equals(patternKey)) {
				String input = createInputForPattern(patternKey, value);
				return ((AssertionFactory<Object>) textFactory).assertion(pattern, input);
			}
		}
		return null;
	}

	private String createInputForPattern(String patternKey, String value) {
		// These inputs match the patterns defined in AssertionFactoryProvider_en.properties
		return switch (patternKey) {
			case StringAssertionFactory.ASSERTION_STRING_EQUALS -> "is equal to " + value;
			case StringAssertionFactory.ASSERTION_STRING_NOT_EQUALS -> "is not equal to " + value;
			case StringAssertionFactory.ASSERTION_STRING_CONTAINS -> "contains " + value;
			case StringAssertionFactory.ASSERTION_STRING_NOT_CONTAINS -> "does not contain " + value;
			case StringAssertionFactory.ASSERTION_STRING_STARTS_WITH -> "starts with " + value;
			case StringAssertionFactory.ASSERTION_STRING_ENDS_WITH -> "ends with " + value;
			case StringAssertionFactory.ASSERTION_STRING_EQUALS_IGNORE_CASE -> "is equal to " + value + " (ignoring case)";
			case StringAssertionFactory.ASSERTION_STRING_CONTAINS_IGNORE_CASE -> "contains " + value + " (ignoring case)";
			case StringAssertionFactory.ASSERTION_GENERIC_NULL -> "is null";
			case StringAssertionFactory.ASSERTION_GENERIC_NOT_NULL -> "is not null";
			default -> value != null ? value : "";
		};
	}
}