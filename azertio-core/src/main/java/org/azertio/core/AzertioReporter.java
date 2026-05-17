package org.azertio.core;

import org.azertio.core.contributors.ReportBuilder;
import java.util.UUID;

public class AzertioReporter {

	private final AzertioRuntime runtime;

	public AzertioReporter(AzertioRuntime runtime) {
		this.runtime = runtime;
	}

	public void report(UUID executionID) {
		runtime.getExtensions(ReportBuilder.class).forEach(builder -> builder.buildReport(executionID));
	}

}
