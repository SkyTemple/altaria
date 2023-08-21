package org.skytemple.altaria.definitions.exceptions;

/**
 * Thrown when an asynchronous operation fails and no fallback operation can be performed.
 */
public class AsyncOperationException extends Exception {
	public AsyncOperationException(String message) {
		super(message);
	}

	public AsyncOperationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AsyncOperationException(Throwable cause) {
		super(cause);
	}
}
