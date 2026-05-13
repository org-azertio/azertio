package org.azertio.core.testplan;

import java.util.UUID;
import java.time.Instant;

public record TestPlan(
	UUID planID,
	UUID projectID,
	Instant createdAt,
	String resourceSetHash,
	String configurationHash,
	UUID planNodeRoot,
	int testCaseCount,
	String suites
) {

}