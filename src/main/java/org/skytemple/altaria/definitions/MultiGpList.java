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

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.skytemple.altaria.utils.Utils;

import java.awt.*;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Used to store a list of users alongside a GP amount they will receive
 */
public class MultiGpList {
	private final Map<Long, Integer> list;

	public MultiGpList() {
		list = new TreeMap<>();
	}

	/**
	 * Removes the user with the specified ID from the list
	 * @param userId User ID
	 */
	public void remove(Long userId) {
		list.remove(userId);
	}

	/**
	 * Given a user ID and an amount, increases the amount of points assigned to the user by the specified amount.
	 * If there isn't an entry for the given user, one will be created with the specified amount.
	 * @param userId User ID
	 * @param points Amount of points to increase the user's entry by, or amount of points to assign to the user if
	 *               no entry for it exists yet.
	 */
	public void add(Long userId, Integer points) {
		list.merge(userId, points, Integer::sum);
	}

	/**
	 * @return Iterator that iterates over the entries on this list
	 */
	public Iterator<Map.Entry<Long, Integer>> iterator() {
		return list.entrySet().iterator();
	}

	/**
	 * Converts the list into an embed. Users will be sorted in descending order by amount of GP.
	 * @return Embed builder with the created embed
	 */
	public EmbedBuilder toEmbed() {
		StringBuilder result = new StringBuilder();
		Map<Long, Integer> sortedGpList = Utils.sortByValue(list, true);
		boolean first = true;

		for (Map.Entry<Long, Integer> entry : sortedGpList.entrySet()) {
			if (first) {
				first = false;
			} else {
				result.append("\n");
			}
			result.append("<@").append(entry.getKey()).append(">: ").append(entry.getValue());
		}

		return new EmbedBuilder()
			.setTitle("Multi-GP list")
			.setDescription(result.toString())
			.setColor(Color.YELLOW);
	}
}
