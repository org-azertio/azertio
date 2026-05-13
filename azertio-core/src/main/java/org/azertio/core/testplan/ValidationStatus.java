package org.azertio.core.testplan;

/**
 * Represents the validation status of a test plan node.
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public enum ValidationStatus {

	OK(0),
	ERROR(1);

	public final int value;

	ValidationStatus(int value) {
		this.value = value;
	}

	public static ValidationStatus of(int value) {
		for (var status : values()) {
			if (status.value == value) return status;
		}
		throw new IllegalArgumentException("Unknown validation status: " + value);
	}
}