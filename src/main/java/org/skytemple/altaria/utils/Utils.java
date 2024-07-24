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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.skytemple.altaria.definitions.singletons.ExtConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class Utils {
	/**
	 * Returns a logger
	 * @param c Class whose name will be used as the logger name
	 * @param level Logging level
	 * @return Logger
	 */
	public static Logger getLogger(Class<?> c, Level level) {
		Logger logger = LogManager.getLogger(c);
		Configurator.setLevel(logger, level);
		return logger;
	}

	/**
	 * Returns a logger with the default logging level.
	 * @param c Class whose name will be used as the logger name
	 * @return Logger
	 */
	public static Logger getLogger(Class<?> c) {
		return getLogger(c, ExtConfig.get().getLogLevel());
	}

	/**
	 * Given a throwable, returns a string that includes its string representation and stacktrace.
	 * @param t Throwable to convert to a string
	 * @return Full string representation of the throwable
	 */
	public static String throwableToStr(Throwable t) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	/**
	 * Converts a double value to an integer. Decimals will be dropped, except if the number is very close to the next
	 * integer, in which case it will be rounded. This is done to prevent values from accidentally being decreased by 1
	 * unit due to rounding errors.
	 * @param value Double value to convert
	 * @return Integer version of the value
	 */
	public static int doubleToInt(Double value) {
		if (Math.abs(value - value.intValue()) > 0.9999999999) {
			return (int) Math.round(value);
		} else {
			return value.intValue();
		}
	}

	/**
	 * Given a map and a series of keys, removes all the keys present on the list from the map.
	 * @param map Map to remove entrires from
	 * @param removeList List containing the entries to remove
	 * @param <K> Map key type
	 * @param <V> Map value type
	 */
	public static <K, V> void removeAll(Map<K, V> map, Iterable<K> removeList) {
		for (K key : removeList) {
			map.remove(key);
		}
	}

	/**
	 * Given a string, truncates it to ensure it doesn't exceed maxChars in length.
	 * The truncation is performed by removing the last lines of the string, ana appending a new one that lists
	 * how many lines were removed.
	 * @param string The string to truncate
	 * @param maxChars The maximum amount of characters the final string can have
	 * @return Final string, with some lines removed if required. Will always have at most maxChar characters.
	 */
	public static String truncateLines(String string, int maxChars) {
		if (string.length() > maxChars) {
			String[] lines = string.split("\n");
			int totalLength = 0;
			int numLines = 0;
			while (totalLength < maxChars) {
				totalLength += lines[numLines].length() + 1; // To account for the line break
				numLines++;
			}
			// Remove the last line if required
			if (totalLength > maxChars) {
				numLines--;
				totalLength -= lines[numLines].length() + 1;
			}
			// Add the extra characters needed to include the message that shows how many lines were truncated
			int truncatedMsgLength = ("\n[+" + (lines.length - numLines) + " lines]").length();
			totalLength += truncatedMsgLength;
			// If we exceed the max, we need to remove more lines
			// The length of the message might change since the amount of removed lines will increase. Assume it does
			// to avoid headaches.
			totalLength += 1;
			while (totalLength > maxChars) {
				numLines--;
				totalLength -= lines[numLines].length() + 1;
			}
			// Build the final string
			String message = String.join("\n", Arrays.copyOfRange(lines, 0, numLines));
			message += "\n[+" + (lines.length - numLines) + " lines]";
			// Double-check that it doesn't exceed the limit
			if (message.length() > maxChars) {
				getLogger(Utils.class).warn("Final truncateLines() message had " + message.length() + " characters!");
				message = message.substring(0, maxChars);
			}
			return message;
		} else {
			return string;
		}
	}

	/**
	 * Given a map, returns a new map where entries are sorted by value.
	 * Based on <a href="https://stackoverflow.com/a/2581754">https://stackoverflow.com/a/2581754</a>.
	 * @param map Map to sort
	 * @param desc True to sort in descending order, false to sort in ascending order
	 * @return New map, sorted by value
	 * @param <K> Type of map keys
	 * @param <V> Type of map values
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, boolean desc) {
		List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
		if (desc) {
			list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
		} else {
			list.sort(Map.Entry.comparingByValue());
		}

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}


	/**
	 * Calculates the logarithm of an arbitrary base for a number
	 * @param base Logarithm base
	 * @param number Number to calculate the logarithm of
	 * @return Logarithm result
	 */
	public static double log(double base, double number) {
		return Math.log(number) / Math.log(base);
	}
}
