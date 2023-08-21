package org.skytemple.altaria.definitions.exceptions;

/**
 * Thrown when a database operation fails for reasons other than a lost connection
 */
public class DbOperationException extends Exception {
	public DbOperationException(String message) {
		super(message);
	}

	public DbOperationException(String message, Throwable cause) {
		super(message, cause);
	}

	public DbOperationException(Throwable cause) {
		super(cause);
	}
}
