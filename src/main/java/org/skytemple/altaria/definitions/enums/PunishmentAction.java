/*
 * Copyright (c) 2023-2024. End45 and other contributors.
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

package org.skytemple.altaria.definitions.enums;

import org.skytemple.altaria.definitions.exceptions.IllegalOperationException;

/**
 * Action that can be automatically performed when someone receives a strike
 */
public enum PunishmentAction {
	NONE,
	KICK,
	MUTE,
	BAN;

	/**
	 * @return True if it makes sense for the current action to have an associated duration
	 */
	public boolean canHaveDuration() {
		return switch (this) {
			case NONE, KICK -> false;
			case MUTE, BAN -> true;
		};
	}

	/**
	 * @return True if the current action must always have a duration specified alongside it
	 */
	public boolean durationRequired() {
		return switch (this) {
			case NONE, KICK, BAN -> false;
			case MUTE -> true;
		};
	}

	/**
	 * If the current action is a verb, returns its past tense.
	 * @return Past tense of the action verb
	 * @throws IllegalOperationException if the current action is not a verb
	 */
	public String getPastTense() {
		return switch (this) {
			case KICK -> "kicked";
			case MUTE -> "muted";
			case BAN -> "banned";
			case NONE -> throw new IllegalOperationException("Action is not a verb");
		};
	}
}
