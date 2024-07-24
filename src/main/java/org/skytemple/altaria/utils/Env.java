/*
 * Copyright (c) 2023-2024. Frostbyte and other contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.skytemple.altaria.utils;

import org.skytemple.altaria.definitions.exceptions.FatalErrorException;

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

	/**
	 * Gets the specified environment variable, as a boolean.
	 * @param name Name of the variable to get
	 * @return Value of the variable. Empty if the variable is not present.
	 * @throws FatalErrorException if the value of the variable cannot be parsed as a boolean.
	 */
	public static Optional<Boolean> getBoolean(String name) {
		String valStr = System.getenv(name);
		if (valStr == null) {
			return Optional.empty();
		} else {
			if (valStr.equals("1") || valStr.equalsIgnoreCase("true")) {
				return Optional.of(true);
			} else if (valStr.equals("0") || valStr.equalsIgnoreCase("false")) {
				return Optional.of(false);
			} else {
				throw new FatalErrorException("Invalid boolean value for env variable \"" + name + "\": " + valStr);
			}
		}
	}
}
