package org.azertio.it;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.backend.ExecutionContext;
import org.azertio.core.contributors.StatisticsProvider;
import org.azertio.core.contributors.StepExpression;
import org.azertio.core.contributors.StepProvider;

@Extension(scope = Scope.TRANSIENT)
public class TestValidationStepProvider implements StepProvider {

	@Override
	public void init(Config config) {}

	@StepExpression("a valid step")
	public void aValidStep() {}

	@StepExpression("a failing step")
	public void aFailingStep() {
		throw new AssertionError("step assertion failed");
	}

	@StepExpression("an error step")
	public void anErrorStep() {
		throw new RuntimeException("step unexpected error");
	}

	@StatisticsProvider
	@StepExpression("a benchmarkable step")
	public void aBenchmarkableStep() {
		ExecutionContext.current().runWithinBenchmark(()->{
			System.out.println("Benchmarking step");
			for (int i = 0; i <10000; i++) {
			}
			return true;
		});
	}

}
