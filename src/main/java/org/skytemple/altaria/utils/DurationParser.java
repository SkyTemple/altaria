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

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to parse strings that represent a time duration.
 */
public class DurationParser {
	private static final Pattern TIME_FORMAT = Pattern.compile("^(\\d+)([smhd])$");

	/**
	 * Given a string that represents a time duration, it as a {@link Duration} object.
	 * The strings to parse are expected to have the format {@code <number><unit>}, with {@code <unit>} being a single
	 * lowercase letter that represents a time unit and {@code <number>} being the amount.
	 * @param str String that represents the duration
	 * @throws IllegalArgumentException If the time string provided does not follow the expected format
	 * @return Duration representing the specified duration
	 */
	public static Duration parse(String str) {
		Matcher matcher = TIME_FORMAT.matcher(str);
		if (matcher.matches()) {
			String num = matcher.group(1);
			String unit = matcher.group(2);
			int numInt;
			try {
				numInt = Integer.parseInt(num);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(e);
			}
			numInt *= getTimeUnitMultiplier(unit);
			return Duration.ofSeconds(numInt);
		} else {
			throw new IllegalArgumentException("Invalid time string");
		}
	}

	/**
	 * Given a string that contains a single character representing a time unit (or an empty string if no time unit was
	 * provided), returns its equivalent in seconds.
	 * @param unit String containing the time unit
	 * @return Number of seconds represented by a single unit of the specified type
	 */
	private static int getTimeUnitMultiplier(String unit) {
		return switch (unit) {
			case "s" -> 1;
			case "m" -> 60;
			case "h" -> 60 * 60;
			case "d" -> 60 * 60 * 24;
			default -> throw new IllegalArgumentException("Invalid time unit");
		};
	}
}
