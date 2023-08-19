package org.skytemple.altaria.db;

/**
 * Class used to perform database operations on the reputation table
 */
public class ReputationDB {
	private static final String REPUTATION_DB_NAME = "rep";

	private final Database db;

	public ReputationDB(Database db) {
		this.db = db;

		// Create the table if it doesn't exist
		db.updateWithReconnect("CREATE TABLE " + REPUTATION_DB_NAME + "(" +
			"`discord_id` BIGINT(30) unsigned NOT NULL," +
			"`points` INT(20) signed NOT NULL," +
			"PRIMARY KEY (`discord_id`));");
	}
}
