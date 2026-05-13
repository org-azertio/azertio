package org.azertio.core;

import org.myjtools.imconfig.Config;
import org.azertio.core.testplan.TestProject;
import org.azertio.core.testplan.TestSuite;
import java.util.List;
import java.util.Optional;

public record AzertioContext(
	TestProject testProject,
	Config configuration,
	List<String> testSuites,
	List<String> plugins
){

	public Optional<TestSuite> testSuite(String name) {
		return testProject.testSuites().stream().filter(suite -> suite.name().equals(name)).findFirst();
	}

}
