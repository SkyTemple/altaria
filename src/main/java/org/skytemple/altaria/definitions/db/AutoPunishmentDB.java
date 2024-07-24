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

import org.skytemple.altaria.definitions.Punishment;
import org.skytemple.altaria.definitions.enums.PunishmentAction;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.exceptions.FatalErrorException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to perform database operations on the auto_timeout table
 */
public class AutoPunishmentDB {
	private static final String AUTO_PUNISHMENT_TABLE = "auto_punishment";

	private final Database db;

	public AutoPunishmentDB(Database db) {
		this.db = db;

		// Create the table if it doesn't exist
		try {
			db.updateWithReconnect("CREATE TABLE IF NOT EXISTS " + AUTO_PUNISHMENT_TABLE + "(" +
				"`strikes` INT(10) UNSIGNED NOT NULL," +
				"`action` ENUM('kick', 'mute', 'ban') NOT NULL," +
				"`duration` INT(10) UNSIGNED," +
				"PRIMARY KEY (`strikes`));");
		} catch (DbOperationException e) {
			throw new FatalErrorException("Cannot create auto_timeout table", e);
		}
	}

	/**
	 * Returns the punishment that should be applied to a user when reaching the given amount of strikes
	 * @param numStrikes Number of strikes
	 * @return Punishment applied when a user receives the specified amount of strikes
	 */
	public Punishment get(int numStrikes) throws DbOperationException {
		try (ResultSet result = new PreparedStatementBuilder(db, "SELECT action, duration FROM " +
			AUTO_PUNISHMENT_TABLE + " WHERE strikes = ?")
			.setInt(numStrikes)
			.executeQuery()) {
			if (result.next()) {
				int duration = result.getInt(2);
				Long actualDuration;
				if (result.wasNull()) {
					actualDuration = null;
				} else {
					actualDuration = (long) duration;
				}
				return new Punishment(PunishmentAction.valueOf(result.getString(1).toUpperCase()), actualDuration);
			} else {
				return new Punishment(PunishmentAction.NONE, 0L);
			}
		} catch (SQLException e) {
			throw new DbOperationException(e);
		}
	}

	/**
	 * @return List of punishments to apply when users are striked and amount of strikes required to apply each of them.
	 */
	public List<StrikesAndPunishment> getAll() throws DbOperationException {
		try (ResultSet result = new PreparedStatementBuilder(db, "SELECT strikes, action, duration FROM " +
			AUTO_PUNISHMENT_TABLE)
			.executeQuery()) {
			List<StrikesAndPunishment> ret = new ArrayList<>();
			while (result.next()) {
				int stikes = result.getInt(1);
				PunishmentAction action = PunishmentAction.valueOf(result.getString(2).toUpperCase());
				int duration = result.getInt(3);
				Long actualDuration;
				if (result.wasNull()) {
					actualDuration = null;
				} else {
					actualDuration = (long) duration;
				}
				ret.add(new StrikesAndPunishment(stikes, new Punishment(action, actualDuration)));
			}
			return ret;
		} catch (SQLException e) {
			throw new DbOperationException(e);
		}
	}

	/**
	 * Sets the punishment to issue to a user when reaching the given number of strikes
	 * @param strikes Number of strikes
	 * @param punishment Punishment to issue
	 */
	public void set(int strikes, Punishment punishment) throws DbOperationException {
		if (punishment.action == PunishmentAction.NONE) {
			new PreparedStatementBuilder(db, "DELETE FROM " + AUTO_PUNISHMENT_TABLE + " WHERE strikes = ?")
				.setInt(strikes)
				.executeUpdate();
		} else {
			PreparedStatementBuilder builder = new PreparedStatementBuilder(db, "UPDATE " + AUTO_PUNISHMENT_TABLE +
				" SET action = ?, duration = ? WHERE strikes = ?")
				.setString(punishment.action.toString().toLowerCase());
			if (punishment.duration == null) {
				builder.setNull(Types.INTEGER);
			} else {
				builder.setInt((int) punishment.duration.toSeconds());
			}
			int result = builder.setInt(strikes)
				.executeUpdate();
			if (result == 0) {
				builder = new PreparedStatementBuilder(db, "INSERT INTO " + AUTO_PUNISHMENT_TABLE +
					"(strikes, action, duration) VALUES(?, ?, ?)")
					.setInt(strikes)
					.setString(punishment.action.toString().toLowerCase());
				if (punishment.duration == null) {
					builder.setNull(Types.INTEGER);
				} else {
					builder.setInt((int) punishment.duration.toSeconds());
				}
				builder.executeUpdate();
			}
		}
	}

	/**
	 * Represents a punishment and the amount of strikes required to apply it
	 */
	public record StrikesAndPunishment(int strikes, Punishment punishment) {}
}
