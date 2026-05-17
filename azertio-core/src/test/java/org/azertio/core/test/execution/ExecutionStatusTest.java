package org.azertio.core.test.execution;

import org.junit.jupiter.api.Test;
import org.azertio.core.execution.ExecutionStatus;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionStatusTest {

	@Test
	void pendingHasValueZero() {
		assertEquals(0, ExecutionStatus.PENDING.value());
		assertEquals(0, ExecutionStatus.PENDING.value);
	}

	@Test
	void runningHasValueOne() {
		assertEquals(1, ExecutionStatus.RUNNING.value());
		assertEquals(1, ExecutionStatus.RUNNING.value);
	}

	@Test
	void finishedHasValueTwo() {
		assertEquals(2, ExecutionStatus.FINISHED.value());
		assertEquals(2, ExecutionStatus.FINISHED.value);
	}

	@Test
	void valuesAreOrdered() {
		var values = ExecutionStatus.values();
		assertEquals(ExecutionStatus.PENDING, values[0]);
		assertEquals(ExecutionStatus.RUNNING, values[1]);
		assertEquals(ExecutionStatus.FINISHED, values[2]);
	}

	@Test
	void valueFieldMatchesValueMethod() {
		for (var status : ExecutionStatus.values()) {
			assertEquals(status.value, status.value());
		}
	}
}