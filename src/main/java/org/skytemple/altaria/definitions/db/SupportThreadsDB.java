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

package org.skytemple.altaria.definitions.db;

import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.exceptions.FatalErrorException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SupportThreadsDB {
	private static final String SUPPORT_THREADS_TABLE_NAME = "support_threads";

	private final Database db;

	public SupportThreadsDB(Database db) {
		this.db = db;

		// Create the table if it doesn't exist
		try {
			db.updateWithReconnect("CREATE TABLE IF NOT EXISTS " + SUPPORT_THREADS_TABLE_NAME + "(" +
				"`user_id` BIGINT(30) UNSIGNED NOT NULL," +
				"`thread_id` BIGINT(30) UNSIGNED NOT NULL," +
				"`should_get_gp` BOOLEAN NOT NULL," + // Internally stored as a 1-byte integer
				"PRIMARY KEY (`user_id`, `thread_id`));");
		} catch (DbOperationException e) {
			throw new FatalErrorException("Cannot create support threads table", e);
		}
	}

	/**
	 * Determines if a user should get GP for their messages in a given support thread
	 * @param userId ID of the user to check
	 * @param threadId ID of the thread to check
	 * @param op True if the user is the person who started the thread, false if it's not.
	 * @return True if the user should get support GP on this thread
	 */
	public boolean shouldUserGetGP(long userId, long threadId, boolean op) throws DbOperationException {
		try (ResultSet result = new PreparedStatementBuilder(db, "SELECT IFNULL((SELECT should_get_gp FROM " +
			SUPPORT_THREADS_TABLE_NAME + " " + "WHERE user_id = ? AND thread_id = ?), 2)")
			.setLong(userId)
			.setLong(threadId)
			.executeQuery()) {
			result.next();
			int resValue = result.getInt(1);

			return switch (resValue) {
				case 0 -> false;
				case 1 -> true;
				case 2 -> !op;
				default -> throw new DbOperationException("Invalid should_get_gp result: " + resValue);
			};
		} catch (SQLException e) {
			throw new DbOperationException(e);
		}
	}

	/**
	 * Returns the ID of the users who should always get GP for their messages in the specified thread
	 * @param threadId Thread ID
	 * @return Users who should always get GP on this thread
	 */
	public List<Long> getShouldGetGpUsers(long threadId) throws DbOperationException {
		return getUsers(threadId, true);
	}

	/**
	 * Returns the ID of the users who should never get GP for their messages in the specified thread
	 * @param threadId Thread ID
	 * @return Users who should never get GP on this thread
	 */
	public List<Long> getShouldNotGetGpUsers(long threadId) throws DbOperationException {
		return getUsers(threadId, false);
	}

	/**
	 * Sets whether the specified user should get GP for their messages in the specified support thread
	 * @param userId User ID
	 * @param threadId Thread ID
	 * @param shouldGetGp True if the user should get GP on the specified thread, false if they shouldn't
	 * @param op True if the user is the person who started the thread, false if it's not.
	 */
	public void setUserSupportGp(long userId, long threadId, boolean shouldGetGp, boolean op) throws DbOperationException {
		if (shouldGetGp ^ op) {
			// Default behavior, remove the entry to save DB space
			new PreparedStatementBuilder(db, "DELETE FROM " + SUPPORT_THREADS_TABLE_NAME +
				" WHERE user_id = ? AND thread_id = ?")
				.setLong(userId)
				.setLong(threadId)
				.executeUpdate();
		} else {
			int result = new PreparedStatementBuilder(db, "UPDATE " + SUPPORT_THREADS_TABLE_NAME + " SET should_get_gp = " +
				"? WHERE user_id = ? AND thread_id = ?")
				.setInt(shouldGetGp ? 1 : 0)
				.setLong(userId)
				.setLong(threadId)
				.executeUpdate();
			if (result == 0) {
				new PreparedStatementBuilder(db, "INSERT INTO " + SUPPORT_THREADS_TABLE_NAME + "(user_id, thread_id, " +
					"should_get_gp) VALUES(?, ?, ?)")
					.setLong(userId)
					.setLong(threadId)
					.setInt(shouldGetGp ? 1 : 0)
					.executeUpdate();
			}
		}
	}

	/**
	 * Returns the ID of the users with "should get GP" overries in the specified thread
	 * @param threadId Thread ID
	 * @param shouldGetGp True to get the users who should always get GP for their messages on the specified thread,
	 *                    false to get the ones who should never get GP.
	 * @return Users who with the specified GP override on this thread
	 */
	private List<Long> getUsers(long threadId, boolean shouldGetGp) throws DbOperationException {
		List<Long> res = new ArrayList<>();
		int shouldGetGpInt = shouldGetGp ? 1 : 0;
		try (ResultSet result = new PreparedStatementBuilder(db, "SELECT user_id FROM " + SUPPORT_THREADS_TABLE_NAME +
			" WHERE thread_id = ? AND should_get_gp = ?")
			.setLong(threadId)
			.setInt(shouldGetGpInt)
			.executeQuery()) {
			while (result.next()) {
				res.add(result.getLong(1));
			}
		} catch (SQLException e) {
			throw new DbOperationException(e);
		}
		return res;
	}
}
