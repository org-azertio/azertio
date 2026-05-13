package org.azertio.core.execution;

import org.azertio.core.AzertioException;

public class NoMatchingStepException extends AzertioException {

	public NoMatchingStepException(String message, Object... args) {
		super(message, args);
	}
}
