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

package org.skytemple.altaria.definitions.db;

import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.exceptions.FatalErrorException;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Class used to perform database operations on the auto_timeout table
 */
public class AutoTimeoutDB {
	private static final String AUTO_TIMEOUT_TABLE = "auto_timeout";

	private final Database db;

	public AutoTimeoutDB(Database db) {
		this.db = db;

		// Create the table if it doesn't exist
		try {
			db.updateWithReconnect("CREATE TABLE IF NOT EXISTS " + AUTO_TIMEOUT_TABLE + "(" +
				"`strikes` INT(10) UNSIGNED NOT NULL," +
				"`duration` INT(10) UNSIGNED NOT NULL," +
				"PRIMARY KEY (`strikes`));");
		} catch (DbOperationException e) {
			throw new FatalErrorException("Cannot create auto_timeout table", e);
		}
	}

	/**
	 * Returns the duration, in seconds, of the timeout that should be applied to a user when reaching the given
	 * amount of strikes
	 * @param numStrikes Number of strikes
	 * @return Timeout duration, in seconds. 0 if no mute punishment is defined for the given number of strikes.
	 */
	public int getDuration(int numStrikes) throws DbOperationException {
		try (ResultSet result = new PreparedStatementBuilder(db, "SELECT IFNULL((SELECT duration FROM " +
			AUTO_TIMEOUT_TABLE + " " + "WHERE strikes = ?), 0)")
			.setInt(numStrikes)
			.executeQuery()) {
			result.next();
			return result.getInt(1);
		} catch (SQLException e) {
			throw new DbOperationException(e);
		}
	}

	/**
	 * Sets the duration of the timeout to give to a user when reaching a given number of strikes
	 * @param strikes Number of strikes
	 * @param duration Timeout duration, in seconds. 0 to remove the punishment.
	 */
	public void setDuration(int strikes, int duration) throws DbOperationException {
		if (duration < 0) {
			throw new IllegalArgumentException("Timeout duration must be positive");
		} else if (duration == 0) {
			new PreparedStatementBuilder(db, "DELETE FROM " + AUTO_TIMEOUT_TABLE + " WHERE strikes = ?")
				.setInt(strikes)
				.executeUpdate();
		} else {
			int result = new PreparedStatementBuilder(db, "UPDATE " + AUTO_TIMEOUT_TABLE + " SET duration = ? " +
				"WHERE strikes = ?")
				.setInt(duration)
				.setInt(strikes)
				.executeUpdate();
			if (result == 0) {
				new PreparedStatementBuilder(db, "INSERT INTO " + AUTO_TIMEOUT_TABLE + "(strikes, duration) " +
					"VALUES(?, ?)")
					.setInt(strikes)
					.setInt(duration)
					.executeUpdate();
			}
		}
	}
}
