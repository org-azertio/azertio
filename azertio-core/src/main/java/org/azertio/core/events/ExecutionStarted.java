package org.azertio.core.events;

import java.time.Instant;
import java.util.UUID;

public record ExecutionStarted(
	Instant instant,
	UUID executionID,
	UUID planID,
	String profile
) implements Event {
}
