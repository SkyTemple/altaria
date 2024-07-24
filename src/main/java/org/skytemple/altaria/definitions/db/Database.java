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

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import org.apache.logging.log4j.Logger;
import org.skytemple.altaria.utils.Utils;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.exceptions.FatalErrorException;

import java.sql.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class used to connect to the bot's database
 */
public class Database {
	// Amount of seconds to wait before determining that a database connection has been lost
	private static final int DB_PING_TIMEOUT = 2;

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
		try {
			connection = connect();
		} catch (DbOperationException e) {
			throw new FatalErrorException(e);
		}
	}

	/**
	 * Runs a database operation that could throw an error. If it does and the error is due to the DB connection
	 * being lost, it will attempt to reconnect if possible.
	 * <br><b>Note</b>: Some operations will not fail immediately if the connection has been lost (for example,
	 * {@link Connection#prepareStatement(String)}), but will throw an error later on, even if a reconnect is performed.
	 * In those cases, it's convenient to run {@link #ensureConnection()} first to check if a reconnection is necessary.
	 * @param dbOperation The operation to run. Can be anything capable of throwing an {@link SQLException}.
	 * @param operation A string that describes the operation performed. Used for error messages.
	 * @throws DbOperationException If the operation throws an error for reasons other than a disconnect, if the
	 * reconnect attempt fails or if the operation throws an error after a successful reconnection.
	 */
	public void runWithReconnect(DatabaseOperation dbOperation, String operation) throws DbOperationException {
		try {
			dbOperation.run(connection);
		} catch (CommunicationsException e) {
			// DB connection lost, reconnect and try again
			logger.warn("Database connection lost. Attempting to reconnect.");
			connection = connect();
			try {
				dbOperation.run(connection);
			} catch (SQLException e2) {
				throw new DbOperationException("Error when retrying DB operation.\nOperation: " + operation, e2);
			}
		} catch (SQLException e) {
			throw new DbOperationException("Error when performing DB operation.\nOperation: " + operation, e);
		}
	}

	/**
	 * Executes a simple SQL query, attempting a reconnection if required. Should not be used for queries with
	 * parameters or that are meant to run multiple times. Use {@link PreparedStatementBuilder} for that.
	 * @param query The query to execute
	 * @return Query result
	 */
	public ResultSet queryWithReconnect(String query) throws DbOperationException {
		AtomicReference<ResultSet> result = new AtomicReference<>();
		runWithReconnect((_connection) -> {
			Statement statement = _connection.createStatement();
			statement.closeOnCompletion();
			result.set(statement.executeQuery(query));
		}, query);
		return result.get();
	}

	/**
	 * Executes a simple SQL update query, attempting a reconnection if required. Should not be used for queries with
	 * parameters or that are meant to run multiple times. Use {@link PreparedStatementBuilder} for that.
	 * @param query The query to execute
	 * @return Number of affected rows, or 0 for statements that return nothing.
	 */
	public int updateWithReconnect(String query) throws DbOperationException {
		AtomicInteger result = new AtomicInteger();
		runWithReconnect((_connection) -> {
			try (Statement statement = _connection.createStatement()) {
				result.set(statement.executeUpdate(query));
			}
		}, query);
		return result.get();
	}

	/**
	 * Checks if the database connection has been lost and reconnects if that's the case.
	 * @throws DbOperationException If the reconnect attempt fails
	 */
	public void ensureConnection() throws DbOperationException {
		boolean fail;
		try {
			fail = !connection.isValid(DB_PING_TIMEOUT);
		} catch (SQLException e) {
			fail = true;
		}
		if (fail) {
			logger.warn("Database ping failed. Attempting to reconnect.");
			connection = connect();
		}
	}

	private Connection connect() throws DbOperationException {
		String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
		try {
			Connection connection = DriverManager.getConnection(url, user, password);
			logger.debug("Database connection successful");
			return connection;
		} catch (SQLException e) {
			throw new DbOperationException("Database connection failed", e);
		}
	}

	/**
	 * Represents a database operation that might throw an {@link SQLException}, potentially due to a lost connection
	 * with the database.
	 */
	@FunctionalInterface
	public interface DatabaseOperation {
		/**
		 * Attempts to perform the database operation
		 * @param connection Database connection
		 * @throws SQLException If the operation fails for whatever reason
		 */
		void run(Connection connection) throws SQLException;
	}
}
