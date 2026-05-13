package org.azertio.core.test.backend;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.AzertioException;
import org.azertio.core.backend.ExecutionContext;
import org.azertio.core.backend.StepProviderBackend;
import org.azertio.core.testplan.DataTable;
import org.azertio.core.testplan.Document;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TestStepProviderBackend {

	static final Config TEST_CONFIG = Config.ofMap(Map.of(
			AzertioConfig.RESOURCE_PATH, "src/test/resources",
			AzertioConfig.ENV_PATH, "target/.azertio"
	));

	@Nested
	class TestsEnglish {
		@Test
		void testRunStepWithOneParameter() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(()->	backend.run("Step with one parameter: 5", Locale.ENGLISH, null, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterFailsWhenNoExpectedAdditionalData() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			DataTable dataTable = new DataTable();
			assertThatCode(() -> backend.run("Step with one parameter: 5", Locale.ENGLISH, dataTable, null))
				.isInstanceOf(AzertioException.class)
				.hasMessageContaining("Step 'stepWithOneParameter' does not expect additional data, but it was provided");
		}

		@Test
		void testRunStepWithoutParameters() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Step without parameters", Locale.ENGLISH, null, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithTwoParameters() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Step with two parameters: 3, 7", Locale.ENGLISH, null, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepNotFoundThrowsException() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Non-existent step", Locale.ENGLISH, null, null))
				.isInstanceOf(AzertioException.class)
				.hasMessageContaining("No matching step found for 'Non-existent step'");
		}

		@Test
		void testRunStepWithOnlyDataTable() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var dataTable = new DataTable(List.of(List.of("col1", "col2"), List.of("val1", "val2")));
			assertThatCode(() -> backend.run("Step with only data table", Locale.ENGLISH, dataTable, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOnlyDocument() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var document = Document.of("text/plain", "hello world");
			assertThatCode(() -> backend.run("Step with only document", Locale.ENGLISH, document, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterAndAssertionPassed() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Step with one parameter and assertion: 5 is greater than 2", Locale.ENGLISH, null, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOnlyAssertionStored() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Step with only assertion: is greater than 2", Locale.ENGLISH, null, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterAndDataTable() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var dataTable = new DataTable(List.of(List.of("col1"), List.of("val1")));
			assertThatCode(() -> backend.run("Step with one parameter and data table: 5", Locale.ENGLISH, dataTable, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testSetUp() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.setUp(null, null, Map.of())).doesNotThrowAnyException();
		}

		@Test
		void testTearDown() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(backend::tearDown).doesNotThrowAnyException();
		}

		@Test
		void testAssertIntegerVariable() {
			assertCoreStepPasses("the integer variable count is equal to 5", "count", "5");
		}

		@Test
		void testAssertDecimalVariable() {
			assertCoreStepPasses("the decimal variable price is equal to 19.99", "price", "19.99");
		}

		@Test
		void testAssertDateVariable() {
			assertCoreStepPasses("the date variable expiryDate is equal to 2025-01-01", "expiryDate", "2025-01-01");
		}

		@Test
		void testAssertTimeVariable() {
			assertCoreStepPasses("the time variable scheduledAt is equal to 09:30:00", "scheduledAt", "09:30:00");
		}

		@Test
		void testAssertDatetimeVariable() {
			assertCoreStepPasses("the datetime variable createdAt is equal to 2025-01-01T10:30:00", "createdAt", "2025-01-01T10:30:00");
		}

		@Test
		void testAssertTextVariable() {
			assertCoreStepPasses("the text variable status contains \"ctiv\"", "status", "active");
		}

		@Test
		void testIsValidStepAndHintsAndListings() {
			var runtime = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(runtime);

			assertThat(backend.isValidStep("Step without parameters", Locale.ENGLISH)).isTrue();
			assertThat(backend.isValidStep("Step without parameterz", Locale.ENGLISH)).isFalse();
			assertThat(backend.allStepsForLocale(Locale.ENGLISH)).isNotEmpty();
			assertThat(backend.allStepsWithLabelForLocale(Locale.ENGLISH))
				.anySatisfy(entry -> assertThat(entry.getKey()).isNotBlank());
			assertThat(backend.hintsForStep("Step without parameterz", Locale.ENGLISH, 3)).isNotEmpty();
		}
	}


	@Nested
	class TestsSpanish {

		static final Locale ES = Locale.forLanguageTag("es");

		@Test
		void testRunStepWithOneParameter() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso con un parámetro: 5", ES, null, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterFailsWhenNoExpectedAdditionalData() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			DataTable dataTable = new DataTable();
			assertThatCode(() -> backend.run("Paso con un parámetro: 5", ES, dataTable, null))
				.isInstanceOf(AzertioException.class)
				.hasMessageContaining("Step 'stepWithOneParameter' does not expect additional data, but it was provided");
		}

		@Test
		void testRunStepWithoutParameters() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso sin parámetros", ES, null, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithTwoParameters() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso con dos parámetros: 3, 7", ES, null, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepNotFoundThrowsException() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso inexistente", ES, null, null))
				.isInstanceOf(AzertioException.class)
				.hasMessageContaining("No matching step found for 'Paso inexistente'");
		}

		@Test
		void testRunStepWithOnlyDataTable() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var dataTable = new DataTable(List.of(List.of("col1", "col2"), List.of("val1", "val2")));
			assertThatCode(() -> backend.run("Paso con solo tabla de datos", ES, dataTable, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOnlyDocument() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var document = Document.of("text/plain", "hola mundo");
			assertThatCode(() -> backend.run("Paso con solo documento", ES, document, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterAndAssertionPassed() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso con un parámetro y una aserción: 5 es mayor que 2", ES, null, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOnlyAssertionStored() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso con solo aserción: es mayor que 2", ES, null, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterAndDataTable() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var dataTable = new DataTable(List.of(List.of("col1"), List.of("val1")));
			assertThatCode(() -> backend.run("Paso con un parámetro y tabla de datos: 5", ES, dataTable, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testSetUp() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.setUp(null, null, Map.of())).doesNotThrowAnyException();
		}

		@Test
		void testTearDown() {
			var cm = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(backend::tearDown).doesNotThrowAnyException();
		}

		@Test
		void testIsValidStepAndHintsAndListings() {
			var runtime = new AzertioRuntime(TEST_CONFIG);
			var backend = new StepProviderBackend(runtime);

			assertThat(backend.isValidStep("Step without parameters", Locale.ENGLISH)).isTrue();
			assertThat(backend.isValidStep("Step without parameterz", Locale.ENGLISH)).isFalse();
			assertThat(backend.allStepsForLocale(Locale.ENGLISH)).isNotEmpty();
			assertThat(backend.allStepsWithLabelForLocale(Locale.ENGLISH))
				.anySatisfy(entry -> assertThat(entry.getKey()).isNotBlank());
			assertThat(backend.hintsForStep("Step without parameterz", Locale.ENGLISH, 3)).isNotEmpty();
		}
	}

	private void assertCoreStepPasses(String step, String variableName, String variableValue) {
		var runtime = new AzertioRuntime(TEST_CONFIG);
		var backend = new StepProviderBackend(runtime);
		backend.setUp(null, null, Map.of());
		try {
			ExecutionContext.current().setVariable(variableName, variableValue);
			assertThatCode(() -> backend.run(step, Locale.ENGLISH, null, null))
				.doesNotThrowAnyException();
		} finally {
			backend.tearDown();
		}
	}


}
