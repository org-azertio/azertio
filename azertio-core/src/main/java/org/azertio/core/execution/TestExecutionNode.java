package org.azertio.core.execution;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class TestExecutionNode {

	private UUID executionID;
	private UUID executionNodeID;
	private UUID planNodeID;
	private ExecutionStatus status;
	private Instant startTime;
	private Instant endTime;
	private ExecutionResult result;
	private String message;
	private Integer testPassedCount;
	private Integer testErrorCount;
	private Integer testFailedCount;


	public long duration() {
		return endTime.toEpochMilli() - startTime.toEpochMilli();
	}


}
