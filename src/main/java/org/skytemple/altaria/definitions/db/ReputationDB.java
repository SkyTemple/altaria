package org.skytemple.altaria.definitions.db;

import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.exceptions.FatalErrorException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

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
				"`discord_id` BIGINT(30) unsigned NOT NULL," +
				"`points` INT(20) signed NOT NULL," +
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
	public int getPoints(long userId) throws DbOperationException {
		try (ResultSet result = new PreparedStatementBuilder(db, "SELECT IFNULL((SELECT points FROM " +
			REPUTATION_TABLE_NAME + " " + "WHERE discord_id = ?), 0)")
			.setLong(userId)
			.executeQuery()) {
			result.next();
			return result.getInt(1);
		} catch (SQLException e) {
			throw new DbOperationException("Error closing ResultSet.", e);
		}
	}

	/**
	 * Gets the amount of points of all the users, sorted by amount (desc)
	 * @return Lis of (user, points) pairs
	 */
	public ArrayList<PointsEntry> getPoints() throws DbOperationException {
		ArrayList<PointsEntry> res = new ArrayList<>();
		try (ResultSet result = db.queryWithReconnect("(SELECT discord_id, points FROM " + REPUTATION_TABLE_NAME +
			" ORDER BY points DESC")) {
			while (result.next()) {
				res.add(new PointsEntry(result.getLong(1), result.getInt(2)));
			}
		} catch (SQLException e) {
			throw new DbOperationException("Error closing ResultSet.", e);
		}
		return res;
	}

	/**
	 * Adds (or removes) points from the specified user
	 * @param userId ID of the user to give the points to
	 * @param amount Amount of points to give
	 */
	public void addPoints(long userId, int amount) throws DbOperationException {
		int result = new PreparedStatementBuilder(db, "UPDATE " + REPUTATION_TABLE_NAME + " SET points = points + ? " +
			"WHERE discord_id = ?")
			.setInt(amount)
			.setLong(userId)
			.executeUpdate();
		if (result == 0) {
			new PreparedStatementBuilder(db, "INSERT INTO " + REPUTATION_TABLE_NAME + "(discord_id, points) " +
				"VALUES(?, ?)")
				.setLong(userId)
				.setInt(amount)
				.executeUpdate();
		}
	}

	/**
	 * Used to return a pair of user ID and points amount
	 */
	public record PointsEntry(long userId, int points) {}
}