package org.skytemple.altaria.db;

import org.apache.logging.log4j.Logger;
import org.skytemple.altaria.Utils;
import org.skytemple.altaria.exceptions.FatalErrorException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Class used to connect to the bot's database
 */
public class Database {
	// Seconds to wait for a DB response before attempting a reconnect
	private static final int DB_TIMEOUT = 5;

	private final String host;
	private final String port;
	private final String user;
	private final String password;
	private final String database;

	private final Logger logger;

	private Connection connection;

	public Database(String host, String port, String user, String password, String database) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.database = database;

		logger = Utils.getLogger(getClass());
		connection = null;
	}

	/**
	 * Gets a connection to the database. If an existing valid connection exists, it will be returned. Otherwise, a new
	 * connection will be created.
	 * @return Database connection
	 */
	public Connection getConnection() {
		try {
			if (connection == null || !connection.isValid(DB_TIMEOUT)) {
				connection = _getConnection();
			}
		} catch (SQLException e) {
			connection = _getConnection();
		}
		return connection;
	}

	private Connection _getConnection() {
		String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
		try {
			Connection connection = DriverManager.getConnection(url, user, password);
			logger.debug("Database connection successful");
			return connection;
		} catch (SQLException e) {
			throw new FatalErrorException("Database connection failed", e);
		}
	}
}
