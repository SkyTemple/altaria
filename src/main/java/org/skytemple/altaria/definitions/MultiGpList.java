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

package org.skytemple.altaria.definitions;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.utils.Utils;

import java.awt.*;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Used to store a list of users alongside a GP amount they will receive
 */
public class MultiGpList implements Iterable<Map.Entry<Long, Double>> {
	private static final String DEFAULT_LIST_NAME = "Multi-GP list";

	private final Map<Long, Double> list;
	private final String listName;

	/**
	 * Creates a new multi-GP list with the default name {@link #DEFAULT_LIST_NAME}
	 */
	public MultiGpList() {
		list = new TreeMap<>();
		listName = DEFAULT_LIST_NAME;
	}

	/**
	 * Creates a new multi-GP list with the given name
	 * @param listName List name
	 */
	public MultiGpList(String listName) {
		list = new TreeMap<>();
		this.listName = listName;
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
	public void add(Long userId, Double points) {
		list.merge(userId, points, Double::sum);
	}

	/**
	 * Given another multi-GP list, adds all the entries from it to the current list, summing the points for each
	 * user.
	 * @param other List to add to the current one
	 */
	public void addAll(MultiGpList other) {
		for (Map.Entry<Long, Double> entry : other) {
			add(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * @return Iterator that iterates over the entries on this list
	 */
	@Override
	public Iterator<Map.Entry<Long, Double>> iterator() {
		return list.entrySet().iterator();
	}

	/**
	 * @return Iterator that iterates over the entries on this list, with GP amounts converted to integers.
	 */
	public Iterator<Map.Entry<Long, Integer>> intIterator() {
		return new ToIntIterator(list.entrySet().iterator());
	}

	/**
	 * Converts the list into an embed. Users will be sorted in descending order by amount of GP.
	 * The name of the list will be displayed on the title of the embed.
	 * @param useIntegers If true, the decimal part of GP amounts will be dropped and the amounts will be displayed
	 *                    as integers. If false, the full decimal value will be displayed.
	 * @return Embed builder with the created embed
	 */
	public EmbedBuilder toEmbed(boolean useIntegers) {
		StringBuilder result = new StringBuilder();
		Map<Long, Double> sortedGpList = Utils.sortByValue(list, true);
		boolean first = true;

		for (Map.Entry<Long, Double> entry : sortedGpList.entrySet()) {
			if (first) {
				first = false;
			} else {
				result.append("\n");
			}
			String valueStr;
			if (useIntegers) {
				int value = Utils.doubleToInt(entry.getValue());
				if (value == 0) {
					valueStr = null;
				} else {
					valueStr = String.valueOf(value);
				}
			} else {
				valueStr = String.valueOf(entry.getValue());
			}

			if (valueStr != null) {
				result.append("<@").append(entry.getKey()).append(">: ").append(valueStr);
			}
		}

		return new EmbedBuilder()
			.setTitle(listName)
			.setDescription(result.toString())
			.setColor(Color.YELLOW);
	}

	/**
	 * Applies the integer version of the amounts stored on the list, altering the GP of all the users listed.
	 * Entries are removed from the list as they are processed. If an error happens, unprocessed entries will not be
	 * removed.
	 * @param rdb Reputation database
	 * @throws DbOperationException If the operation fails due to a database error
	 */
	public void apply(ReputationDB rdb) throws DbOperationException {
		Iterator<Map.Entry<Long, Integer>> it = intIterator();
		while (it.hasNext()) {
			Map.Entry<Long, Integer> entry = it.next();
			int points = entry.getValue();
			if (points != 0) {
				rdb.addPoints(entry.getKey(), entry.getValue());
			}
			it.remove();
		}
	}

	/**
	 * Used to convert an iterator that gets GP entries as doubles to one that gets them as integers. Decimals are
	 * dropped in the conversion.
	 * @param baseIterator Original iterator
	 */
	private record ToIntIterator(Iterator<Map.Entry<Long, Double>> baseIterator)
		implements Iterator<Map.Entry<Long, Integer>> {
		@Override
		public boolean hasNext() {
			return baseIterator.hasNext();
		}

		@Override
		public Map.Entry<Long, Integer> next() {
			Map.Entry<Long, Double> oldEntry = baseIterator.next();
			return new AbstractMap.SimpleEntry<>(oldEntry.getKey(), Utils.doubleToInt(oldEntry.getValue()));
		}

		@Override
		public void remove() {
			baseIterator.remove();
		}
	}
}
