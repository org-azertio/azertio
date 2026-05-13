package org.azertio.core.test.backend;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.Assertion;
import org.azertio.core.contributors.StepProvider;
import org.azertio.core.testplan.DataTable;
import org.azertio.core.testplan.Document;
import org.azertio.core.contributors.SetUp;
import org.azertio.core.contributors.StepExpression;
import org.azertio.core.contributors.TearDown;

@Extension(scope = Scope.TRANSIENT)
public class TestStepProvider implements StepProvider  {


	public boolean runnedStepWithoutParameters = false;
	public Integer runnedStepWithOneParameter = null;
	public Integer runnedStepWithTwoParameters1 = null;
	public Integer runnedStepWithTwoParameters2 = null;
	public Assertion runnedStepWithOnlyAssertion = null;
	public Integer runnedStepWithOneParameterAndAssertionNumber = null;
	public DataTable runnedStepWithOnlyDataTable = null;
	public Integer runnedStepWithOneParameterAndDataTableNumber = null;
	public Document runnedStepWithOnlyDocument = null;

	@Override
	public void init(Config config) {
		// no-op for tests
	}

	@SetUp(order = 1)
	public void setup() {
		// This method is intentionally left empty for testing purposes.
	}


	@TearDown(order = 2)
	public void tearDown() {
		// This method is intentionally left empty for testing purposes.
	}


   @StepExpression("stepWithoutParameters")
   public void stepWithoutParameters() {
	  this.runnedStepWithoutParameters = true;
   }

	@StepExpression("stepWithOneParameter")
	public void stepWithOneParameter(Integer number) {
		this.runnedStepWithOneParameter = number;
	}

	@StepExpression(value = "stepWithTwoParameters", args = { "number1:integer", "number2:integer" })
	public void stepWithTwoParameters(Integer number1, Integer number2) {
		this.runnedStepWithTwoParameters1 = number1;
		this.runnedStepWithTwoParameters2 = number2;
	}

	@StepExpression(value = "stepWithOneParameterAndAssertion")
	public void stepWithOneParameterAndAssertion(Integer number, Assertion assertion) {
		this.runnedStepWithOneParameterAndAssertionNumber = number;
		Assertion.assertThat(number, assertion);
	}

	@StepExpression(value = "stepWithOnlyDataTable")
	public void stepWithOnlyDataTable(DataTable dataTable) {
		this.runnedStepWithOnlyDataTable = dataTable;
	}

	@StepExpression(value = "stepWithOneParameterAndDataTable")
	public void stepWithOneParameterAndDataTable(Integer number, DataTable dataTable) {
		this.runnedStepWithOneParameterAndDataTableNumber = number;
	}

	@StepExpression(value = "stepWithOnlyDocument")
	public void stepWithOnlyDocument(Document document) {
		this.runnedStepWithOnlyDocument = document;
	}

	@StepExpression(value = "stepWithOnlyAssertion")
	public void stepWithOnlyAssertion(Assertion assertion) {
		this.runnedStepWithOnlyAssertion = assertion;
	}

	@StepExpression(value = "stepThatAlwaysFails")
	public void stepThatAlwaysFails() {
		throw new AssertionError("This step is designed to always fail");
	}

	@StepExpression(value = "stepWithUnexpectedError")
	public void stepWithUnexpectedError() {
		throw new IllegalArgumentException("This step is designed to throw an unexpected error");
	}


}
