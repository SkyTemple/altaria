package org.skytemple.altaria.definitions.exceptions;

/**
 * Thrown when an attempted operation is invalid given the current situation.
 */
public class IllegalOperationException extends RuntimeException {
	public IllegalOperationException(String message) {
		super(message);
	}

	public IllegalOperationException(String message, Throwable cause) {
		super(message, cause);
	}

	public IllegalOperationException(Throwable cause) {
		super(cause);
	}
}
