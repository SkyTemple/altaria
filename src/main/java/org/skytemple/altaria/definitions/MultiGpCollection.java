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

package org.skytemple.altaria.definitions;

import java.util.Map;
import java.util.TreeMap;

/**
 * Stores a collection of multi-GP lists, each identified by a user ID
 */
public class MultiGpCollection {
	private final Map<Long, MultiGpList> lists;

	public MultiGpCollection() {
		lists = new TreeMap<>();
	}

	/**
	 * Returns the multi-GP list associated to the given user ID. If no list is associated to it, a new one is created
	 * and returned.
	 * @param userId User ID
	 * @return List associated to the given user ID. A new one will be created if required.
	 */
	public MultiGpList getOrNew(Long userId) {
		return lists.getOrDefault(userId, new MultiGpList());
	}

	/**
	 * Returns the multi-GP list associated to the given user ID, or null if no list is associated to it.
	 * @param userId User ID
	 * @return List associated to the given ID, or null if it doesn't exist.
	 */
	public MultiGpList get(Long userId) {
		return lists.get(userId);
	}

	/**
	 * Adds a new multi-GP list to the collection, identified by a user ID.
	 * @param userId ID of the user the list is associated to
	 * @param list List to add
	 */
	public void put(Long userId, MultiGpList list) {
		lists.put(userId, list);
	}

	/**
	 * Deletes the multi-GP list associated to the given user ID
	 * @param userId User ID
	 */
	public void remove(Long userId) {
		lists.remove(userId);
	}
}
