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
import java.util.ArrayList;
import java.util.List;

/**
 * Used to convert a Duration to certain string formats
 */
public class DurationFormatter {
	private final Duration duration;
	// The duration, split into time units
	private final long days;
	private final int hours;
	private final int minutes;
	private final int seconds;

	public DurationFormatter(Duration duration) {
		this.duration = duration;
		days = duration.toDaysPart();
		hours = duration.toHoursPart();
		minutes = duration.toMinutesPart();
		seconds = duration.toSecondsPart();
	}

	/**
	 * @return Duration converted to a user-readable format
	 */
	public String toUserFormat() {
		if (duration.isZero()) {
			return "0 seconds";
		} else {
			List<String> stringParts = new ArrayList<>();
			if (days > 0) {
				stringParts.add(days + (days == 1 ? " day" : " days"));
			}
			if (hours > 0) {
				stringParts.add(hours + (hours == 1 ? " hour" : " hours"));
			}
			if (minutes > 0) {
				stringParts.add(minutes + (minutes == 1 ? " minute" : " minutes"));
			}
			if (seconds > 0) {
				stringParts.add(seconds + (seconds == 1 ? " second" : " seconds"));
			}

			if (stringParts.size() > 1) {
				stringParts.set(stringParts.size() - 1, " and " + stringParts.get(stringParts.size() - 1));
			}
			for (int i = stringParts.size() - 2; i >= 1; i--) {
				stringParts.set(i, ", " + stringParts.get(i));
			}
			return String.join("", stringParts);
		}
	}

	/**
	 * @return Duration converted to a format that can be understood by Vortex
	 */
	public String toVortexFormat() {
		if (duration.isZero()) {
			return "0";
		} else {
			StringBuilder builder = new StringBuilder();
			if (days > 0) {
				builder.append(days).append("d");
			}
			if (hours > 0) {
				builder.append(hours).append("h");
			}
			if (minutes > 0) {
				builder.append(minutes).append("m");
			}
			if (seconds > 0) {
				builder.append(seconds).append("s");
			}
			return builder.toString();
		}
	}
}
