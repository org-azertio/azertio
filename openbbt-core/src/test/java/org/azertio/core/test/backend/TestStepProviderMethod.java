package org.azertio.core.test.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.azertio.core.Assertion;
import org.azertio.core.DataType;
import org.azertio.core.DataTypes;
import org.azertio.core.AzertioException;
import org.azertio.core.backend.StepProviderMethod;
import org.azertio.core.testplan.DataTable;
import org.azertio.core.testplan.Document;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TestStepProviderMethod {


	private static final Assertion passingAssertion = new Assertion() {
		@Override
		public boolean test(Object actualValue) {
			return true;
		}
		@Override
		public String describeFailure(Object actualValue) {
			return "";
		}
	};

	private static final Assertion assertion = new Assertion() {
		@Override
		public boolean test(Object actualValue) {
			return false;
		}
		@Override
		public String describeFailure(Object actualValue) {
			return "";
		}
	};

	private static final DataType dataType = new DataType() {
		@Override
		public String name() {
			return "integer";
		}
		@Override
		public Class<?> javaType() {
			return Integer.class;
		}
		@Override
		public String hint() {
			return "";
		}
		@Override
		public Pattern pattern() {
			return null;
		}
		@Override
		public Object parse(String value) {
			return Integer.parseInt(value);
		}
	};


	@Nested
	class TestRunStepWithoutParameters {

		@DisplayName("Test run step without parameters")
		@Test
		public void testRunStepWithoutParameters() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithoutParameters");
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
					stepProvider,
					method,
					DataTypes.of()
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithoutParameters");
			Map<String, Object> arguments = Map.of();
			assertThat(stepProvider.runnedStepWithoutParameters).isFalse();
			assertThatCode(() -> stepProviderMethod.run(arguments, null)).doesNotThrowAnyException();
			assertThat(stepProvider.runnedStepWithoutParameters).isTrue();
		}


		@DisplayName("Test run step without parameters fails when passing an argument")
		@Test
		public void testErrorStepWithoutParametersPassedArgument() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithoutParameters");
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
					stepProvider,
					method,
					DataTypes.of()
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithoutParameters");
			Map<String, Object> arguments = Map.of("integer", 42);
			assertThatCode(
					() -> stepProviderMethod.run(arguments, null)
			)
					.isInstanceOf(AzertioException.class)
					.hasMessage("Step 'stepWithoutParameters' expects 0 arguments, but 1 were provided");
		}


		@DisplayName("Test run step without parameters fails when passing a datatable")
		@Test
		public void testErrorStepWithoutParametersPassedDataTable() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithoutParameters");
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
					stepProvider,
					method,
					DataTypes.of()
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithoutParameters");
			Map<String, Object> arguments = Map.of();
			DataTable datatable = new DataTable(List.of(List.of("column1"), List.of("value1")));
			assertThatCode(
					() -> stepProviderMethod.run(arguments, datatable)
			)
					.isInstanceOf(AzertioException.class)
					.hasMessage("Step 'stepWithoutParameters' does not expect additional data, but it was provided");
		}


		@DisplayName("Test run step without parameters fails when passing an assertion")
		@Test
		public void testErrorStepWithoutParametersPassedAssertion() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithoutParameters");
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
					stepProvider,
					method,
					DataTypes.of()
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithoutParameters");
			Map<String, Object> arguments = Map.of();
			assertThatCode(
					() -> stepProviderMethod.run(arguments, assertion)
			)
					.isInstanceOf(AzertioException.class)
					.hasMessage("Step 'stepWithoutParameters' does not expect additional data, but it was provided");
		}

	}


	@Nested
	class TestRunStepWithOneParameter {

		@DisplayName("Test run step with one parameter")
		@Test
		public void testRunStepWithOneParameter() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameter", Integer.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithOneParameter");
			Map<String, Object> arguments = Map.of("integer", 42);
			assertThat(stepProvider.runnedStepWithOneParameter).isNull();
			assertThatCode(() -> stepProviderMethod.run(arguments, null)).doesNotThrowAnyException();
			assertThat(stepProvider.runnedStepWithOneParameter).isEqualTo(42);
		}


		@DisplayName("Test run step with one parameter fails when datatype is not provided")
		@Test
		public void testRunStepWithOneParameterNoDatatypes() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameter", Integer.class);
			assertThatCode(() -> new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of()
			)).isInstanceOf(AzertioException.class)
			.hasMessageStartingWith("Unknown data type for java type class java.lang.Integer");

		}


		@DisplayName("Test run step without parameters fails when incorrect argument type is used")
		@Test
		public void testRunStepWithOneParameterIncorrectType() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameter", Integer.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithOneParameter");
			Map<String, Object> arguments = Map.of("integer", 42L);
			assertThatCode(() -> stepProviderMethod.run(arguments, null))
				.isInstanceOf(AzertioException.class)
				.hasMessage("Argument 'integer' has type Long, but expected type is Integer");
		}


		@DisplayName("Test run step with one parameter fails when passing a datatable")
		@Test
		public void testRunStepWithOneParameterPassedDataTable() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameter", Integer.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
					stepProvider,
					method,
					DataTypes.of(dataType)
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithOneParameter");
			Map<String, Object> arguments = Map.of();
			DataTable datatable = new DataTable(List.of(List.of("column1"), List.of("value1")));
			assertThatCode(
					() -> stepProviderMethod.run(arguments, datatable)
			)
					.isInstanceOf(AzertioException.class)
					.hasMessage("Step 'stepWithOneParameter' expects 1 arguments, but 0 were provided");
		}


		@DisplayName("Test run step with one parameter fails when passing an argument and datatable")
		@Test
		public void testRunStepWithOneParameterPassedArgumentAndDataTable() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameter", Integer.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
					stepProvider,
					method,
					DataTypes.of(dataType)
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithOneParameter");
			Map<String, Object> arguments = Map.of("integer", 42);
			DataTable datatable = new DataTable(List.of(List.of("column1"), List.of("value1")));
			assertThatCode(
					() -> stepProviderMethod.run(arguments, datatable)
			)
					.isInstanceOf(AzertioException.class)
					.hasMessage("Step 'stepWithOneParameter' does not expect additional data, but it was provided");
		}


		@DisplayName("Test run step without parameters fails when passing an assertion")
		@Test
		public void testRunStepWithOneParameterPassedAssertion() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameter", Integer.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
					stepProvider,
					method,
					DataTypes.of(dataType)
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithOneParameter");
			Map<String, Object> arguments = Map.of();
			assertThatCode(
					() -> stepProviderMethod.run(arguments, assertion)
			)
					.isInstanceOf(AzertioException.class)
					.hasMessage("Step 'stepWithOneParameter' expects 1 arguments, but 0 were provided");
		}

	}


	@Nested
	class TestRunStepWithTwoParameters {

		@DisplayName("Test run step with two parameters")
		@Test
		public void testRunStepWithTwoParameters() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithTwoParameters", Integer.class, Integer.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithTwoParameters");
			Map<String, Object> arguments = Map.of("number1", 10, "number2", 20);
			assertThat(stepProvider.runnedStepWithTwoParameters1).isNull();
			assertThat(stepProvider.runnedStepWithTwoParameters2).isNull();
			assertThatCode(() -> stepProviderMethod.run(arguments, null)).doesNotThrowAnyException();
			assertThat(stepProvider.runnedStepWithTwoParameters1).isEqualTo(10);
			assertThat(stepProvider.runnedStepWithTwoParameters2).isEqualTo(20);
		}


		@DisplayName("Test run step with two parameters fails when datatype is not provided")
		@Test
		public void testRunStepWithTwoParametersNoDatatypes() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithTwoParameters", Integer.class, Integer.class);
			assertThatCode(() -> new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of()
			)).isInstanceOf(AzertioException.class)
			.hasMessageStartingWith("Unknown data type integer");
		}


		@DisplayName("Test run step with two parameters fails when one argument is missing")
		@Test
		public void testRunStepWithTwoParametersMissingOneArgument() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithTwoParameters", Integer.class, Integer.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			Map<String, Object> arguments = Map.of("number1", 10);
			assertThatCode(() -> stepProviderMethod.run(arguments, null))
				.isInstanceOf(AzertioException.class)
				.hasMessage("Step 'stepWithTwoParameters' expects 2 arguments, but 1 were provided");
		}


		@DisplayName("Test run step with two parameters fails when incorrect argument type is used")
		@Test
		public void testRunStepWithTwoParametersIncorrectType() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithTwoParameters", Integer.class, Integer.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			Map<String, Object> arguments = Map.of("number1", 10L, "number2", 20L);
			assertThatCode(() -> stepProviderMethod.run(arguments, null))
				.isInstanceOf(AzertioException.class)
				.hasMessage("Argument 'number1' has type Long, but expected type is Integer");
		}


		@DisplayName("Test run step with two parameters fails when passing a datatable")
		@Test
		public void testRunStepWithTwoParametersPassedDataTable() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithTwoParameters", Integer.class, Integer.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			Map<String, Object> arguments = Map.of("number1", 10, "number2", 20);
			DataTable datatable = new DataTable(List.of(List.of("column1"), List.of("value1")));
			assertThatCode(() -> stepProviderMethod.run(arguments, datatable))
				.isInstanceOf(AzertioException.class)
				.hasMessage("Step 'stepWithTwoParameters' does not expect additional data, but it was provided");
		}

	}


	@Nested
	class TestRunStepWithOnlyAssertion {

		@DisplayName("Test run step with only assertion")
		@Test
		public void testRunStepWithOnlyAssertion() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOnlyAssertion", Assertion.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of()
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithOnlyAssertion");
			assertThat(stepProvider.runnedStepWithOnlyAssertion).isNull();
			assertThatCode(() -> stepProviderMethod.run(Map.of(), assertion)).doesNotThrowAnyException();
			assertThat(stepProvider.runnedStepWithOnlyAssertion).isNotNull();
		}


		@DisplayName("Test run step with only assertion fails when passing an extra argument")
		@Test
		public void testRunStepWithOnlyAssertionPassedArgument() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOnlyAssertion", Assertion.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			Map<String, Object> arguments = Map.of("integer", 42);
			assertThatCode(() -> stepProviderMethod.run(arguments, assertion))
				.isInstanceOf(AzertioException.class)
				.hasMessage("Step 'stepWithOnlyAssertion' expects 0 arguments, but 1 were provided");
		}

	}


	@Nested
	class TestRunStepWithOneParameterAndAssertion {

		@DisplayName("Test run step with one parameter and assertion")
		@Test
		public void testRunStepWithOneParameterAndAssertion() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameterAndAssertion", Integer.class, Assertion.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithOneParameterAndAssertion");
			Map<String, Object> arguments = Map.of("integer", 42);
			assertThat(stepProvider.runnedStepWithOneParameterAndAssertionNumber).isNull();
			assertThatCode(() -> stepProviderMethod.run(arguments, passingAssertion)).doesNotThrowAnyException();
			assertThat(stepProvider.runnedStepWithOneParameterAndAssertionNumber).isEqualTo(42);
		}


		@DisplayName("Test run step with one parameter and assertion fails when argument is missing")
		@Test
		public void testRunStepWithOneParameterAndAssertionMissingArgument() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameterAndAssertion", Integer.class, Assertion.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			assertThatCode(() -> stepProviderMethod.run(Map.of(), passingAssertion))
				.isInstanceOf(AzertioException.class)
				.hasMessage("Step 'stepWithOneParameterAndAssertion' expects 1 arguments, but 0 were provided");
		}


		@DisplayName("Test run step with one parameter and assertion fails when incorrect argument type is used")
		@Test
		public void testRunStepWithOneParameterAndAssertionIncorrectType() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameterAndAssertion", Integer.class, Assertion.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			Map<String, Object> arguments = Map.of("integer", 42L);
			assertThatCode(() -> stepProviderMethod.run(arguments, passingAssertion))
				.isInstanceOf(AzertioException.class)
				.hasMessage("Argument 'integer' has type Long, but expected type is Integer");
		}

	}


	@Nested
	class TestRunStepWithOnlyDataTable {

		@DisplayName("Test run step with only datatable")
		@Test
		public void testRunStepWithOnlyDataTable() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOnlyDataTable", DataTable.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of()
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithOnlyDataTable");
			DataTable datatable = new DataTable(List.of(List.of("column1"), List.of("value1")));
			assertThat(stepProvider.runnedStepWithOnlyDataTable).isNull();
			assertThatCode(() -> stepProviderMethod.run(Map.of(), datatable)).doesNotThrowAnyException();
			assertThat(stepProvider.runnedStepWithOnlyDataTable).isNotNull();
		}


		@DisplayName("Test run step with only datatable fails when passing an extra argument")
		@Test
		public void testRunStepWithOnlyDataTablePassedArgument() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOnlyDataTable", DataTable.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			DataTable datatable = new DataTable(List.of(List.of("column1"), List.of("value1")));
			Map<String, Object> arguments = Map.of("integer", 42);
			assertThatCode(() -> stepProviderMethod.run(arguments, datatable))
				.isInstanceOf(AzertioException.class)
				.hasMessage("Step 'stepWithOnlyDataTable' expects 0 arguments, but 1 were provided");
		}

	}


	@Nested
	class TestRunStepWithOneParameterAndDataTable {

		@DisplayName("Test run step with one parameter and datatable")
		@Test
		public void testRunStepWithOneParameterAndDataTable() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameterAndDataTable", Integer.class, DataTable.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithOneParameterAndDataTable");
			DataTable datatable = new DataTable(List.of(List.of("column1"), List.of("value1")));
			Map<String, Object> arguments = Map.of("integer", 42);
			assertThat(stepProvider.runnedStepWithOneParameterAndDataTableNumber).isNull();
			assertThatCode(() -> stepProviderMethod.run(arguments, datatable)).doesNotThrowAnyException();
			assertThat(stepProvider.runnedStepWithOneParameterAndDataTableNumber).isEqualTo(42);
		}


		@DisplayName("Test run step with one parameter and datatable fails when argument is missing")
		@Test
		public void testRunStepWithOneParameterAndDataTableMissingArgument() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameterAndDataTable", Integer.class, DataTable.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			DataTable datatable = new DataTable(List.of(List.of("column1"), List.of("value1")));
			assertThatCode(() -> stepProviderMethod.run(Map.of(), datatable))
				.isInstanceOf(AzertioException.class)
				.hasMessage("Step 'stepWithOneParameterAndDataTable' expects 1 arguments, but 0 were provided");
		}


		@DisplayName("Test run step with one parameter and datatable fails when incorrect argument type is used")
		@Test
		public void testRunStepWithOneParameterAndDataTableIncorrectType() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOneParameterAndDataTable", Integer.class, DataTable.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			DataTable datatable = new DataTable(List.of(List.of("column1"), List.of("value1")));
			Map<String, Object> arguments = Map.of("integer", 42L);
			assertThatCode(() -> stepProviderMethod.run(arguments, datatable))
				.isInstanceOf(AzertioException.class)
				.hasMessage("Argument 'integer' has type Long, but expected type is Integer");
		}

	}


	@Nested
	class TestRunStepWithOnlyDocument {

		@DisplayName("Test run step with only document")
		@Test
		public void testRunStepWithOnlyDocument() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOnlyDocument", Document.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of()
			);
			assertThat(stepProviderMethod.stepKey()).isEqualTo("stepWithOnlyDocument");
			Document document = new Document("text/plain", "content");
			assertThat(stepProvider.runnedStepWithOnlyDocument).isNull();
			assertThatCode(() -> stepProviderMethod.run(Map.of(), document)).doesNotThrowAnyException();
			assertThat(stepProvider.runnedStepWithOnlyDocument).isNotNull();
		}


		@DisplayName("Test run step with only document fails when passing an extra argument")
		@Test
		public void testRunStepWithOnlyDocumentPassedArgument() throws NoSuchMethodException {
			TestStepProvider stepProvider = new TestStepProvider();
			Method method = stepProvider.getClass().getMethod("stepWithOnlyDocument", Document.class);
			StepProviderMethod stepProviderMethod = new StepProviderMethod(
				stepProvider,
				method,
				DataTypes.of(dataType)
			);
			Document document = new Document("text/plain", "content");
			Map<String, Object> arguments = Map.of("integer", 42);
			assertThatCode(() -> stepProviderMethod.run(arguments, document))
				.isInstanceOf(AzertioException.class)
				.hasMessage("Step 'stepWithOnlyDocument' expects 0 arguments, but 1 were provided");
		}

	}

}
