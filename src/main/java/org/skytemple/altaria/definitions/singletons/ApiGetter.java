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

package org.skytemple.altaria.definitions.singletons;

import org.javacord.api.DiscordApi;
import org.skytemple.altaria.definitions.exceptions.IllegalOperationException;

/**
 * Class used to store a single {@link DiscordApi} instance and retrieve it from anywhere.
 */
public class ApiGetter {
	private static DiscordApi api;

	protected ApiGetter() {}

	/**
	 * Initializes the object. Must be called before any calls to the {@link #get()} method happen.
	 * Calling this method multiple times has no effect.
	 * @param api Discord API object, already initialized.
	 */
	public static void init(DiscordApi api) {
		if (ApiGetter.api == null) {
			ApiGetter.api = api;
		}
	}

	/**
	 * Gets the instance of this object. {@link #init} must be called at least once first.
	 * @return The object's single instance
	 */
	public static DiscordApi get() {
		if (api == null) {
			throw new IllegalOperationException("ApiGetter must be initialized before get() can be called");
		} else {
			//noinspection StaticVariableUsedBeforeInitialization
			return api;
		}
	}
}
