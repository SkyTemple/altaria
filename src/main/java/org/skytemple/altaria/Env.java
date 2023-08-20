package org.skytemple.altaria;

import java.util.Optional;

/**
 * Class used to retrieve data stored in environment variables.
 */
public class Env {
	/**
	 * Gets the specified environment variable, as a string.
	 * @param name Name of the variable to get
	 * @return Value of the variable. Empty if the variable is not present.
	 */
	public static Optional<String> getString(String name) {
		return Optional.ofNullable(System.getenv(name));
	}

	/**
	 * Gets the specified environment variable, as an integer.
	 * @param name Name of the variable to get
	 * @return Value of the variable. Empty if the variable is not present.
	 * @throws NumberFormatException if the value of the variable cannot be parsed as an integer.
	 */
	public static Optional<Integer> getInt(String name) {
		String valStr = System.getenv(name);
		if (valStr == null) {
			return Optional.empty();
		} else {
			return Optional.of(Integer.parseInt(valStr));
		}
	}

	/**
	 * Gets the specified environment variable, as a long.
	 * @param name Name of the variable to get
	 * @return Value of the variable. Empty if the variable is not present.
	 * @throws NumberFormatException if the value of the variable cannot be parsed as a long.
	 */
	public static Optional<Long> getLong(String name) {
		String valStr = System.getenv(name);
		if (valStr == null) {
			return Optional.empty();
		} else {
			return Optional.of(Long.parseLong(valStr));
		}
	}
}
