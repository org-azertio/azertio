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
public class TestExecution {

	private UUID executionID;
	private UUID planID;
	private UUID executionRootNodeID;
	private Instant executedAt;
	private Integer testPassedCount;
	private Integer testErrorCount;
	private Integer testFailedCount;
	private String profile;

}