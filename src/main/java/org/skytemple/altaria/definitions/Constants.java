/*
 * Copyright (c) 2023. End45 and other contributors.
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

/**
 * Stores global constants
 */
public class Constants {
	// Maximum allowed characters for a Discord message
	public static final int MAX_DISCORD_MSG_CHARS = 2000;
	// Maximum slowmode delay allowed by Discord
	public static final int MAX_SLOWMODE_TIME = 21600;
	// Time to wait for an action to complete before aborting. Used when responding to interactions, since they
	// currently have a maximum response time of 3 seconds.
	public static final int ACTION_TIMEOUT = 2;
}
