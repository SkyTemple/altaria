package org.skytemple.altaria.definitions.exceptions;

/**
 * Thrown whenever the bot runs into an error situation in which it cannot keep running
 */
public class FatalErrorException extends RuntimeException {
	public FatalErrorException(String message) {
		super(message);
	}

	public FatalErrorException(String message, Throwable cause) {
		super(message, cause);
	}

	public FatalErrorException(Throwable cause) {
		super(cause);
	}
}
