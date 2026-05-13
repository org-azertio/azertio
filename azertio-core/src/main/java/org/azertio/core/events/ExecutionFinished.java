package org.azertio.core.events;

import org.azertio.core.execution.ExecutionResult;
import java.time.Instant;
import java.util.UUID;

public record ExecutionFinished(
	Instant instant,
	UUID executionID,
	UUID planID,
	String profile,
	ExecutionResult result
) implements Event {
}
