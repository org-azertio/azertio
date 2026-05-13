package org.azertio.core.events;

import java.time.Instant;
import java.util.UUID;

public record ExecutionNodeFinished(
		Instant instant,
		UUID executionID,
		UUID executionNodeID,
		UUID testPlanNodeID,
		org.azertio.core.execution.ExecutionResult result) implements Event {

}