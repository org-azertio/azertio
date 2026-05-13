package org.azertio.core.test.assertions;

import org.junit.jupiter.api.Test;
import org.azertio.core.AssertionFactories;
import org.azertio.core.AssertionFactory;
import org.azertio.core.AzertioException;
import org.azertio.core.assertions.CoreAssertionFactories;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AssertionFactories}.
 */
class TestAssertionFactories {

	static AssertionFactories factories = AssertionFactories.of(CoreAssertionFactories.ALL);

	@Test
	void coreEnglish_shouldContainAllDefaultFactories() {
		assertThat(factories.byName("integer-assertion")).isNotNull();
		assertThat(factories.byName("decimal-assertion")).isNotNull();
		assertThat(factories.byName("date-assertion")).isNotNull();
		assertThat(factories.byName("time-assertion")).isNotNull();
		assertThat(factories.byName("datetime-assertion")).isNotNull();
		assertThat(factories.byName("text-assertion")).isNotNull();
	}

	@Test
	void byName_withValidName_shouldReturnFactory() {
		AssertionFactory<?> factory = factories.byName("integer-assertion");

		assertThat(factory).isNotNull();
		assertThat(factory.name()).isEqualTo("integer-assertion");
	}

	@Test
	void byName_withInvalidName_shouldThrowException() {
		assertThatThrownBy(() -> factories.byName("non-existent-assertion"))
			.isInstanceOf(AzertioException.class)
			.hasMessageContaining("Unknown assertion")
			.hasMessageContaining("non-existent-assertion");
	}

	@Test
	void of_withVarargs_shouldCreateRegistry() {
		AssertionFactory<?> numberFactory = factories.byName("integer-assertion");
		AssertionFactory<?> textFactory = factories.byName("text-assertion");

		AssertionFactories custom = AssertionFactories.of(numberFactory, textFactory);

		assertThat(custom.byName("integer-assertion")).isNotNull();
		assertThat(custom.byName("text-assertion")).isNotNull();
		assertThatThrownBy(() -> custom.byName("date-assertion"))
			.isInstanceOf(AzertioException.class);
	}
}