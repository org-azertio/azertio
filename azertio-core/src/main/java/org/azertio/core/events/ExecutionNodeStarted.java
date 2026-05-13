package org.azertio.core.events;

import java.time.Instant;
import java.util.UUID;

public record ExecutionNodeStarted(
		Instant instant,
		UUID executionID,
		UUID executionNodeID,
		UUID testPlanNodeID
	) implements Event {

}
