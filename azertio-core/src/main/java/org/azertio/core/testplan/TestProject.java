package org.azertio.core.testplan;

import java.util.List;

public record TestProject(
	String name,
	String description,
	String organization,
	List<TestSuite> testSuites
) {


}
