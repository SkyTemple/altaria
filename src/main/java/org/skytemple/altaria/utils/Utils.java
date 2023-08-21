package org.skytemple.altaria.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.skytemple.altaria.Main;
import org.skytemple.altaria.definitions.singletons.ExtConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public class Utils {
	/**
	 * Returns a logger
	 * @param c Class whose name will be used as the logger name
	 * @param level Logging level
	 * @return Logger
	 */
	public static Logger getLogger(Class<?> c, Level level) {
		Logger logger = LogManager.getLogger(Main.class);
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
}
