package org.azertio.core.execution;

public enum ExecutionResult {

	PASSED(1),
	FAILED(2),
	SKIPPED(3),
	ERROR(4),
	UNDEFINED(5);

	final int value;

	private static final ExecutionResult[] VALUES = ExecutionResult.values();

	ExecutionResult(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}

	public static ExecutionResult of(int value) {
		for (var result : VALUES) {
			if (result.value == value) {
				return result;
			}
		}
		throw new IllegalArgumentException("Unknown ExecutionResult value: " + value);
	}

}
