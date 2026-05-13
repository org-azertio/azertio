package org.azertio.core.testplan;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.azertio.core.execution.ExecutionResult;
import java.time.Instant;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@Getter @Setter @EqualsAndHashCode
public class
TestPlanNodeExecution {

	private ExecutionResult result;
	private Instant startInstant;
	private Instant finishInstant;

}
