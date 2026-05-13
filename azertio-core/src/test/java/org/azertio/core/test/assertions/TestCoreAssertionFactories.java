package org.azertio.core.test.assertions;

import org.junit.jupiter.api.Test;
import org.azertio.core.AssertionFactory;
import org.azertio.core.assertions.AssertionMessageProvider;
import org.azertio.core.assertions.CoreAssertionFactories;
import org.azertio.core.messages.Messages;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CoreAssertionFactories}.
 */
class TestCoreAssertionFactories {

	@Test
	void assertionFactories_shouldReturnAllCoreFactories() {
		Messages messages = Messages.of(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories).hasSize(6);
	}

	@Test
	void assertionFactories_shouldContainIntegerAssertion() {
		Messages messages = Messages.of(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("integer-assertion");
	}

	@Test
	void assertionFactories_shouldContainDecimalAssertion() {
		Messages messages = Messages.of(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("decimal-assertion");
	}

	@Test
	void assertionFactories_shouldContainDateAssertion() {
		Messages messages = Messages.of(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("date-assertion");
	}

	@Test
	void assertionFactories_shouldContainTimeAssertion() {
		Messages messages = Messages.of(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("time-assertion");
	}

	@Test
	void assertionFactories_shouldContainDatetimeAssertion() {
		Messages messages = Messages.of(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("datetime-assertion");
	}

	@Test
	void assertionFactories_shouldContainTextAssertion() {
		Messages messages = Messages.of(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("text-assertion");
	}

	@Test
	void assertionFactories_allFactoriesHaveNames() {
		Messages messages = Messages.of(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.containsExactlyInAnyOrder(
				"integer-assertion",
				"decimal-assertion",
				"date-assertion",
				"time-assertion",
				"datetime-assertion",
				"text-assertion"
			);
	}
}