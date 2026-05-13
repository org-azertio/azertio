package org.azertio.core.test.assertions;

import org.junit.jupiter.api.Test;
import org.azertio.core.Assertion;
import org.azertio.core.AssertionFactories;
import org.azertio.core.AssertionFactory;
import org.azertio.core.AssertionPattern;
import org.azertio.core.assertions.CoreAssertionFactories;
import org.azertio.core.assertions.TemporalAssertionFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TemporalAssertionFactory}.
 */
class TestTemporalAssertionFactory {

	static AssertionFactories factories = AssertionFactories.of(CoreAssertionFactories.ALL);
	
	@Test
	void dateFactory_name_shouldReturnFactoryName() {
		AssertionFactory<?> dateFactory = factories.byName("date-assertion");
		assertThat(dateFactory.name()).isEqualTo("date-assertion");
	}

	@Test
	void timeFactory_name_shouldReturnFactoryName() {
		AssertionFactory<?> timeFactory = factories.byName("time-assertion");
		assertThat(timeFactory.name()).isEqualTo("time-assertion");
	}

	@Test
	void datetimeFactory_name_shouldReturnFactoryName() {
		AssertionFactory<?> datetimeFactory = factories.byName("datetime-assertion");
		assertThat(datetimeFactory.name()).isEqualTo("datetime-assertion");
	}

	@Test
	void dateFactory_patterns_shouldReturnPatterns() {
		AssertionFactory<?> dateFactory = factories.byName("date-assertion");
		List<? extends AssertionPattern<?>> patterns = dateFactory.patterns(Locale.ENGLISH);

		assertThat(patterns).isNotEmpty();
	}

	@Test
	void dateAssertion_equals_shouldMatchEqualDates() {
		AssertionFactory<?> dateFactory = factories.byName("date-assertion");
		Assertion assertion = findAndCreateAssertion(dateFactory,
			TemporalAssertionFactory.ASSERTION_TEMPORAL_EQUALS, "2024-01-15");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(LocalDate.of(2024, 1, 15))).isTrue();
		assertThat(assertion.test(LocalDate.of(2024, 1, 16))).isFalse();
	}

	@Test
	void dateAssertion_after_shouldMatchLaterDates() {
		AssertionFactory<?> dateFactory = factories.byName("date-assertion");
		Assertion assertion = findAndCreateAssertion(dateFactory,
			TemporalAssertionFactory.ASSERTION_TEMPORAL_AFTER, "2024-01-15");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(LocalDate.of(2024, 1, 16))).isTrue();
		assertThat(assertion.test(LocalDate.of(2024, 1, 15))).isFalse();
		assertThat(assertion.test(LocalDate.of(2024, 1, 14))).isFalse();
	}

	@Test
	void dateAssertion_before_shouldMatchEarlierDates() {
		AssertionFactory<?> dateFactory = factories.byName("date-assertion");
		Assertion assertion = findAndCreateAssertion(dateFactory,
			TemporalAssertionFactory.ASSERTION_TEMPORAL_LESS, "2024-01-15");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(LocalDate.of(2024, 1, 14))).isTrue();
		assertThat(assertion.test(LocalDate.of(2024, 1, 15))).isFalse();
		assertThat(assertion.test(LocalDate.of(2024, 1, 16))).isFalse();
	}

	@Test
	void dateAssertion_afterOrEquals_shouldMatchLaterOrEqualDates() {
		AssertionFactory<?> dateFactory = factories.byName("date-assertion");
		Assertion assertion = findAndCreateAssertion(dateFactory,
			TemporalAssertionFactory.ASSERTION_TEMPORAL_AFTER_EQUALS, "2024-01-15");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(LocalDate.of(2024, 1, 16))).isTrue();
		assertThat(assertion.test(LocalDate.of(2024, 1, 15))).isTrue();
		assertThat(assertion.test(LocalDate.of(2024, 1, 14))).isFalse();
	}

	@Test
	void dateAssertion_null_shouldMatchNullValue() {
		AssertionFactory<?> dateFactory = factories.byName("date-assertion");
		Assertion assertion = findAndCreateAssertion(dateFactory,
			TemporalAssertionFactory.ASSERTION_GENERIC_NULL, null);

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(null)).isTrue();
		assertThat(assertion.test(LocalDate.now())).isFalse();
	}

	@Test
	void timeAssertion_equals_shouldMatchEqualTimes() {
		AssertionFactory<?> timeFactory = factories.byName("time-assertion");
		Assertion assertion = findAndCreateAssertion(timeFactory,
			TemporalAssertionFactory.ASSERTION_TEMPORAL_EQUALS, "10:30:00");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(LocalTime.of(10, 30, 0))).isTrue();
		assertThat(assertion.test(LocalTime.of(10, 31, 0))).isFalse();
	}

	@Test
	void timeAssertion_after_shouldMatchLaterTimes() {
		AssertionFactory<?> timeFactory = factories.byName("time-assertion");
		Assertion assertion = findAndCreateAssertion(timeFactory,
			TemporalAssertionFactory.ASSERTION_TEMPORAL_AFTER, "10:30:00");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(LocalTime.of(11, 0, 0))).isTrue();
		assertThat(assertion.test(LocalTime.of(10, 30, 0))).isFalse();
		assertThat(assertion.test(LocalTime.of(10, 0, 0))).isFalse();
	}

	@Test
	void datetimeAssertion_equals_shouldMatchEqualDateTimes() {
		AssertionFactory<?> datetimeFactory = factories.byName("datetime-assertion");
		Assertion assertion = findAndCreateAssertion(datetimeFactory,
			TemporalAssertionFactory.ASSERTION_TEMPORAL_EQUALS, "2024-01-15T10:30:00");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(LocalDateTime.of(2024, 1, 15, 10, 30, 0))).isTrue();
		assertThat(assertion.test(LocalDateTime.of(2024, 1, 15, 10, 31, 0))).isFalse();
	}

	@Test
	void datetimeAssertion_after_shouldMatchLaterDateTimes() {
		AssertionFactory<?> datetimeFactory = factories.byName("datetime-assertion");
		Assertion assertion = findAndCreateAssertion(datetimeFactory,
			TemporalAssertionFactory.ASSERTION_TEMPORAL_AFTER, "2024-01-15T10:30:00");

		assertThat(assertion).isNotNull();
		assertThat(assertion.test(LocalDateTime.of(2024, 1, 15, 11, 0, 0))).isTrue();
		assertThat(assertion.test(LocalDateTime.of(2024, 1, 16, 10, 30, 0))).isTrue();
		assertThat(assertion.test(LocalDateTime.of(2024, 1, 15, 10, 30, 0))).isFalse();
	}

	@SuppressWarnings("unchecked")
	private Assertion findAndCreateAssertion(AssertionFactory<?> factory, String patternKey, String value) {
		List<AssertionPattern<Object>> patterns =
			(List<AssertionPattern<Object>>) (List<?>) factory.patterns(Locale.ENGLISH);

		for (AssertionPattern<Object> pattern : patterns) {
			if (pattern.key().equals(patternKey)) {
				String input = createInputForPattern(patternKey, value);
				return ((AssertionFactory<Object>) factory).assertion(pattern, input);
			}
		}
		return null;
	}

	private String createInputForPattern(String patternKey, String value) {
		// These inputs match the patterns defined in AssertionFactoryProvider_en.properties
		return switch (patternKey) {
			case TemporalAssertionFactory.ASSERTION_TEMPORAL_EQUALS -> "is equal to " + value;
			case TemporalAssertionFactory.ASSERTION_TEMPORAL_AFTER -> "is after " + value;
			case TemporalAssertionFactory.ASSERTION_TEMPORAL_LESS -> "is before " + value;
			case TemporalAssertionFactory.ASSERTION_TEMPORAL_AFTER_EQUALS -> "is after or equal to " + value;
			case TemporalAssertionFactory.ASSERTION_TEMPORAL_LESS_EQUALS -> "is before or equal to " + value;
			case TemporalAssertionFactory.ASSERTION_TEMPORAL_NOT_EQUALS -> "is not equal to " + value;
			case TemporalAssertionFactory.ASSERTION_GENERIC_NULL -> "is null";
			case TemporalAssertionFactory.ASSERTION_GENERIC_NOT_NULL -> "is not null";
			default -> value != null ? value : "";
		};
	}
}