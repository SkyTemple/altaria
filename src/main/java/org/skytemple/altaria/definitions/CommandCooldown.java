/*
 * Copyright (c) 2025. Frostbyte and other contributors.
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

package org.skytemple.altaria.definitions;

import java.util.Map;
import java.util.TreeMap;

/**
 * Used to implement a per-user cooldown system for a command
 */
public class CommandCooldown {
	// Maps user IDs to the next time they can use the command again. The time is stored as a timestamp, in seconds.
	private final Map<Long, Long> nextCommandUsage;

	/**
	 * Creates a new CommandCooldown object.
	 */
	public CommandCooldown() {
		nextCommandUsage = new TreeMap<>();
	}

	/**
	 * Returns the remaining cooldown time for the given user
	 * @param userId ID of the user to return the cooldown for
	 * @return Remaining cooldown for the given user, in seconds. 0 if the user is not in cooldown.
	 */
	public long remainingCooldown(long userId) {
		Long nextUsageTime = nextCommandUsage.get(userId);
		if (nextUsageTime == null) {
			return 0;
		}

		long currentTime = System.currentTimeMillis() / 1000;
		if (currentTime > nextUsageTime) {
			nextCommandUsage.remove(userId);
			return 0;
		} else {
			return nextUsageTime - currentTime;
		}
	}

	/**
	 * Marks the user as being in cooldown mode for the set amount of seconds
	 * @param userId User ID
	 * @param seconds Seconds to cooldown for
	 */
	public void setCooldown(long userId, long seconds) {
		if (seconds > 0) {
			nextCommandUsage.put(userId, System.currentTimeMillis() / 1000 + seconds);
		}
	}
}
