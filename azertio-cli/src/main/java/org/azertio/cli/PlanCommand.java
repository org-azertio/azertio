package org.azertio.cli;

import org.azertio.core.AzertioContext;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.persistence.TestPlanRepositoryWriter;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.util.Log;
import picocli.CommandLine;

@CommandLine.Command(
	name = "plan",
	description = "Analyze the test plan"
)
public final class PlanCommand extends AbstractCommand {

	private static final Log log = Log.of();

	@CommandLine.Option(
		names = {"--detail"},
		description = "Show detailed analysis of the test plan",
		defaultValue = "false"
	)
	boolean detail;


	@Override
	protected void execute() {

		AzertioContext context = getContext();
		AzertioRuntime runtime = new AzertioRuntime(context.configuration());
		try {
			TestPlan testPlan = runtime.buildTestPlan(context, getSelectedSuites());
			log.info("{}", testPlan.planID());
			if (detail) {
				TestPlanRepositoryWriter writer = new TestPlanRepositoryWriter(
						runtime.getRepository(TestPlanRepository.class)
				);
				writer.write(testPlan.planNodeRoot(), System.out::print);
			}
		} catch (Exception e) {
			log.warn("No test plan assembled: {}", e.getMessage());
		}
	}


}
