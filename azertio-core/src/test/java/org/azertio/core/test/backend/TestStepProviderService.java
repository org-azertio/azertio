package org.azertio.core.test.backend;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.AssertionFactories;
import org.azertio.core.DataTypes;
import org.azertio.core.AzertioException;
import org.azertio.core.assertions.CoreAssertionFactories;
import org.azertio.core.backend.StepProviderService;
import org.azertio.core.datatypes.CoreDataTypes;
import org.azertio.core.messages.Messages;
import org.azertio.core.testplan.DataTable;
import org.azertio.core.testplan.Document;
import org.azertio.core.test.TestMessages;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TestStepProviderService {


    static final DataTypes dataTypes = DataTypes.of(CoreDataTypes.ALL);
    static final AssertionFactories assertionFactories = AssertionFactories.of(CoreAssertionFactories.ALL);

    static final Messages stepMessages = new TestMessages(
        Map.of(Locale.ENGLISH, Map.of(
            "stepWithoutParameters", "Step without parameters",
            "stepWithOneParameter", "Step with one parameter: {integer}",
            "stepWithTwoParameters", "Step with two parameters: {number1:integer}, {number2:integer}",
            "stepWithOneParameterAndAssertion", "Step with one parameter and assertion: {integer} {{integer-assertion}}",
            "stepWithOnlyDataTable", "Step with only data table",
            "stepWithOnlyDocument", "Step with only document"
        )
    ));

    @Test
    void testStepNoMatch() throws Throwable {
        TestStepProvider stepProvider = new TestStepProvider();
        StepProviderService service = new StepProviderService(
            stepProvider,
            dataTypes,
            assertionFactories,
            stepMessages
        );
        var matchingStep = service.matchingStep("Step with one parameter: 5x", Locale.ENGLISH);
        assertThat(matchingStep).isEmpty();
    }


    @Test
    void testStepWithOneParameterLiteralValue() throws Throwable {
        TestStepProvider stepProvider = new TestStepProvider();
	    StepProviderService service = new StepProviderService(
			stepProvider,
		    dataTypes,
		    assertionFactories,
		    stepMessages
	    );
		var matchingStep = service.matchingStep("Step with one parameter: 5", Locale.ENGLISH)
            .orElseThrow();
		var stepMethod = matchingStep.left();
        var match = matchingStep.right();

        assertThat(stepMethod.stepKey()).isEqualTo("stepWithOneParameter");

        Map<String,Object> arguments = match.interpolateArguments(Map.of());
        assertThat(stepProvider.runnedStepWithOneParameter).isNull();
        stepMethod.run(arguments,null);
        assertThat(stepProvider.runnedStepWithOneParameter).isEqualTo(5);
      }


    @Test
    void testStepWithOneParameterVariable() throws Throwable {
        TestStepProvider stepProvider = new TestStepProvider();
        StepProviderService service = new StepProviderService(
            stepProvider,
            dataTypes,
            assertionFactories,
            stepMessages
        );
        var matchingStep = service.matchingStep("Step with one parameter: ${var1}", Locale.ENGLISH)
                .orElseThrow();
        var stepMethod = matchingStep.left();
        var match = matchingStep.right();
        assertThat(stepMethod.stepKey()).isEqualTo("stepWithOneParameter");
        Map<String,Object> arguments = match.interpolateArguments(Map.of("var1",5));
        assertThat(stepProvider.runnedStepWithOneParameter).isNull();
        stepMethod.run(arguments,null);
        assertThat(stepProvider.runnedStepWithOneParameter).isEqualTo(5);
    }


    @Test
    void testStepWithOneParameterVariableWithWrongType() throws Throwable {
        TestStepProvider stepProvider = new TestStepProvider();
        StepProviderService service = new StepProviderService(
            stepProvider,
            dataTypes,
            assertionFactories,
            stepMessages
        );
        var matchingStep = service.matchingStep("Step with one parameter: ${var1}", Locale.ENGLISH)
                .orElseThrow();
        var stepMethod = matchingStep.left();
        var match = matchingStep.right();
        assertThat(stepMethod.stepKey()).isEqualTo("stepWithOneParameter");
        Map<String,Object> arguments = match.interpolateArguments(Map.of("var1",5L));
        assertThat(stepProvider.runnedStepWithOneParameter).isNull();
        assertThatCode( ()-> stepMethod.run(arguments,null) )
            .isInstanceOf(AzertioException.class)
            .hasMessage("Argument 'integer' has type Long, but expected type is Integer");

    }


    @Test
    void testStepWithOneParameter() throws Throwable {
        TestStepProvider stepProvider = new TestStepProvider();
        StepProviderService service = new StepProviderService(
            stepProvider,
            dataTypes,
            assertionFactories,
            stepMessages
        );
        var matchingStep = service.matchingStep("Step with one parameter: 5", Locale.ENGLISH)
                .orElseThrow();
        var stepMethod = matchingStep.left();
        var match = matchingStep.right();

        assertThat(stepMethod.stepKey()).isEqualTo("stepWithOneParameter");

        assertThat(stepProvider.runnedStepWithOneParameter).isNull();
        stepMethod.run(match.interpolateArguments(Map.of()),null);
        assertThat(stepProvider.runnedStepWithOneParameter).isEqualTo(5);
    }



    @Test
    void stepWithOneParameterAndAssertion() throws Throwable {
        TestStepProvider stepProvider = new TestStepProvider();
        var stepContributorBackend = new StepProviderService(stepProvider, dataTypes, assertionFactories, stepMessages);
        stepContributorBackend.setUp(Config.empty());
        var step = stepContributorBackend.matchingStep("Step with one parameter and assertion: 5 is greater than 2", Locale.ENGLISH).orElseThrow();
        var runnableStep = step.left();
        var match = step.right();
        assertThat(runnableStep.stepKey()).isEqualTo("stepWithOneParameterAndAssertion");
        assertThat(stepProvider.runnedStepWithOneParameterAndAssertionNumber).isNull();
        runnableStep.run(match.interpolateArguments(Map.of()),match.assertion());
        assertThat(stepProvider.runnedStepWithOneParameterAndAssertionNumber).isEqualTo(5);
    }


    @Test
    void testStepWithoutParameters() throws Throwable {
        var stepProvider = new TestStepProvider();
        var service = new StepProviderService(stepProvider, dataTypes, assertionFactories, stepMessages);
        var matchingStep = service.matchingStep("Step without parameters", Locale.ENGLISH).orElseThrow();
        var stepMethod = matchingStep.left();
        var match = matchingStep.right();
        assertThat(stepMethod.stepKey()).isEqualTo("stepWithoutParameters");
        assertThat(stepProvider.runnedStepWithoutParameters).isFalse();
        stepMethod.run(match.interpolateArguments(Map.of()), null);
        assertThat(stepProvider.runnedStepWithoutParameters).isTrue();
    }


    @Test
    void testStepWithTwoParameters() throws Throwable {
        var stepProvider = new TestStepProvider();
        var service = new StepProviderService(stepProvider, dataTypes, assertionFactories, stepMessages);
        var matchingStep = service.matchingStep("Step with two parameters: 3, 7", Locale.ENGLISH).orElseThrow();
        var stepMethod = matchingStep.left();
        var match = matchingStep.right();
        assertThat(stepMethod.stepKey()).isEqualTo("stepWithTwoParameters");
        assertThat(stepProvider.runnedStepWithTwoParameters1).isNull();
        assertThat(stepProvider.runnedStepWithTwoParameters2).isNull();
        stepMethod.run(match.interpolateArguments(Map.of()), null);
        assertThat(stepProvider.runnedStepWithTwoParameters1).isEqualTo(3);
        assertThat(stepProvider.runnedStepWithTwoParameters2).isEqualTo(7);
    }


    @Test
    void testStepWithTwoParametersVariables() throws Throwable {
        var stepProvider = new TestStepProvider();
        var service = new StepProviderService(stepProvider, dataTypes, assertionFactories, stepMessages);
        var matchingStep = service.matchingStep("Step with two parameters: ${n1}, ${n2}", Locale.ENGLISH).orElseThrow();
        var stepMethod = matchingStep.left();
        var match = matchingStep.right();
        assertThat(stepMethod.stepKey()).isEqualTo("stepWithTwoParameters");
        assertThat(stepProvider.runnedStepWithTwoParameters1).isNull();
        assertThat(stepProvider.runnedStepWithTwoParameters2).isNull();
        stepMethod.run(match.interpolateArguments(Map.of("n1", 3, "n2", 7)), null);
        assertThat(stepProvider.runnedStepWithTwoParameters1).isEqualTo(3);
        assertThat(stepProvider.runnedStepWithTwoParameters2).isEqualTo(7);
    }


    @Test
    void testStepWithOnlyDataTable() throws Throwable {
        var stepProvider = new TestStepProvider();
        var service = new StepProviderService(stepProvider, dataTypes, assertionFactories, stepMessages);
        var matchingStep = service.matchingStep("Step with only data table", Locale.ENGLISH).orElseThrow();
        var stepMethod = matchingStep.left();
        var match = matchingStep.right();
        assertThat(stepMethod.stepKey()).isEqualTo("stepWithOnlyDataTable");
        var dataTable = new DataTable(List.of(List.of("a", "b"), List.of("1", "2")));
        assertThat(stepProvider.runnedStepWithOnlyDataTable).isNull();
        stepMethod.run(match.interpolateArguments(Map.of()), dataTable);
        assertThat(stepProvider.runnedStepWithOnlyDataTable).isEqualTo(dataTable);
    }


    @Test
    void testStepWithOnlyDocument() throws Throwable {
        var stepProvider = new TestStepProvider();
        var service = new StepProviderService(stepProvider, dataTypes, assertionFactories, stepMessages);
        var matchingStep = service.matchingStep("Step with only document", Locale.ENGLISH).orElseThrow();
        var stepMethod = matchingStep.left();
        var match = matchingStep.right();
        assertThat(stepMethod.stepKey()).isEqualTo("stepWithOnlyDocument");
        var document = Document.of("text/plain", "hello world");
        assertThat(stepProvider.runnedStepWithOnlyDocument).isNull();
        stepMethod.run(match.interpolateArguments(Map.of()), document);
        assertThat(stepProvider.runnedStepWithOnlyDocument).isEqualTo(document);
    }


    @Test
    void testSetUp() {
        var service = new StepProviderService(new TestStepProvider(), dataTypes, assertionFactories, stepMessages);
        service.setUp(Config.empty());
    }


    @Test
    void testTearDown() {
        var service = new StepProviderService(new TestStepProvider(), dataTypes, assertionFactories, stepMessages);
        service.tearDown();
    }
}
