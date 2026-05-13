package org.azertio.core;

import java.io.Serial;

/**
 * AzertioException is the base class for all exceptions thrown by the Azertio API.
 * It extends RuntimeException, allowing it to be used as an unchecked exception.
 * This class provides constructors for creating exceptions with a message, a cause,
 * or both, and supports formatted messages.

 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
public class AzertioException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;


	public AzertioException(String message) {
		super(message);
	}

	public AzertioException(String message, Object... args) {
		super(format(message,args));
	}

	public AzertioException(Throwable cause, String message) {
		super(message, cause);
	}

	public AzertioException(Throwable cause, String message, Object... args) {
		super(format(message,args), cause);
	}

	public AzertioException(Throwable cause) {
		super(cause);
	}


	protected static String format(String message, Object... args) {
		if (args == null || args.length == 0) {
			return message;
		}
		String formattedMessage = message;
		for (Object arg : args) {
			formattedMessage = formattedMessage.replaceFirst(
				"\\{}",
				arg == null ? "<null>" : String.valueOf(arg)
			);
		}
		return formattedMessage;
	}

}
