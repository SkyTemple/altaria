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
import org.skytemple.altaria.utils.Utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to perform database operations on the reputation table
 */
public class ReputationDB {
	private static final String REPUTATION_TABLE_NAME = "rep";

	private final Database db;

	public ReputationDB(Database db) {
		this.db = db;

		// Create the table if it doesn't exist
		try {
			db.updateWithReconnect("CREATE TABLE IF NOT EXISTS " + REPUTATION_TABLE_NAME + "(" +
				"`discord_id` BIGINT(30) UNSIGNED NOT NULL," +
				"`points` DOUBLE SIGNED NOT NULL," +
				"PRIMARY KEY (`discord_id`));");
		} catch (DbOperationException e) {
			throw new FatalErrorException("Cannot create reputation table", e);
		}
	}

	/**
	 * Returns the amount of points a certain user has
	 * @param userId User to check
	 * @return User points
	 */
	public double getPoints(long userId) throws DbOperationException {
		try (ResultSet result = new PreparedStatementBuilder(db, "SELECT IFNULL((SELECT points FROM " +
			REPUTATION_TABLE_NAME + " " + "WHERE discord_id = ?), 0)")
			.setLong(userId)
			.executeQuery()) {
			result.next();
			return result.getDouble(1);
		} catch (SQLException e) {
			throw new DbOperationException(e);
		}
	}

	/**
	 * Returns the amount of points a certain user has, as an integer
	 * @param userId User to check
	 * @return User points, rounded down to the nearest integer
	 */
	public int getPointsInt(long userId) throws DbOperationException {
		return Utils.doubleToInt(getPoints(userId));
	}

	/**
	 * Gets the amount of points of all the users, sorted by amount (desc)
	 * @return Lis of (user, points) pairs
	 */
	public List<PointsEntry> getPoints() throws DbOperationException {
		List<PointsEntry> res = new ArrayList<>();
		try (ResultSet result = db.queryWithReconnect("SELECT discord_id, points FROM " + REPUTATION_TABLE_NAME +
			" ORDER BY points DESC")) {
			while (result.next()) {
				res.add(new PointsEntry(result.getLong(1), result.getDouble(2)));
			}
		} catch (SQLException e) {
			throw new DbOperationException(e);
		}
		return res;
	}

	/**
	 * Gets the amount of points of all the users, as an integer, sorted by amount (desc)
	 * @return Lis of (user, points) pairs
	 */
	public List<PointsEntryInt> getPointsInt() throws DbOperationException {
		List<PointsEntryInt> res = new ArrayList<>();
		for (PointsEntry entry : getPoints()) {
			res.add(new PointsEntryInt(entry.userId, Utils.doubleToInt(entry.points)));
		}
		return res;
	}

	/**
	 * Adds (or removes) points from the specified user
	 * @param userId ID of the user to give the points to
	 * @param amount Amount of points to give
	 */
	public void addPoints(long userId, double amount) throws DbOperationException {
		int result = new PreparedStatementBuilder(db, "UPDATE " + REPUTATION_TABLE_NAME + " SET points = points + ? " +
			"WHERE discord_id = ?")
			.setDouble(amount)
			.setLong(userId)
			.executeUpdate();
		if (result == 0) {
			new PreparedStatementBuilder(db, "INSERT INTO " + REPUTATION_TABLE_NAME + "(discord_id, points) " +
				"VALUES(?, ?)")
				.setLong(userId)
				.setDouble(amount)
				.executeUpdate();
		}
	}

	/**
	 * Used to return a pair of user ID and points amount (as a double)
	 */
	public record PointsEntry(long userId, double points) {}

	/**
	 * Used to return a pair of user ID and points amount (as an integer)
	 */
	public record PointsEntryInt(long userId, int points) {}
}
