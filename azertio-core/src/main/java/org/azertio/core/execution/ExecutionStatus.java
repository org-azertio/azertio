package org.azertio.core.execution;

public enum ExecutionStatus {

	PENDING(0),
	RUNNING(1),
	FINISHED(2);


	public final int value;

	ExecutionStatus(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}

}
