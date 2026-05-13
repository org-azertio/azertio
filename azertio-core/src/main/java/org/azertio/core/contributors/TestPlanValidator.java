package org.azertio.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.azertio.core.backend.StepProviderBackend;
import org.azertio.core.testplan.TestPlan;

@ExtensionPoint(version = "1.0")
public interface TestPlanValidator {

	/**
	 * Validates the given test plan and updates its validation status and messages.
	 * This method should check each node in the test plan for correctness
	 * @param plan The test plan to validate
	 * @param backend The backend to use for validating steps
	 */
	void validate(TestPlan plan, StepProviderBackend backend);

}
