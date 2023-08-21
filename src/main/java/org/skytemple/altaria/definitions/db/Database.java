package org.skytemple.altaria.definitions.db;

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
		connection = connect();
	}

	/**
	 * Runs a database operation that could throw an error. If it does and the error is due to the DB connection
	 * being lost, it will attempt to reconnect if possible.
	 * @param dbOperation The operation to run. Can be anything capable of throwing an {@link SQLException}.
	 * @param operation A string that describes the operation performed. Used for error messages.
	 * @throws DbOperationException If the operation throws an error for reasons other than a disconnect.
	 */
	public void runWithReconnect(DatabaseOperation dbOperation, String operation) throws DbOperationException {
		try {
			dbOperation.run(connection);
		} catch (SQLException e) {
			// TODO: Check if the connection was lost and attempt to reconnect. If the reconnection fails, throw
			//  a fatal error. If the exception was caused by something other than a reconnection, throw
			//  DBOperationException.
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
			try (Statement statement = _connection.createStatement()) {
				result.set(statement.executeQuery(query));
			}
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

	private Connection connect() {
		String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
		try {
			Connection connection = DriverManager.getConnection(url, user, password);
			logger.debug("Database connection successful");
			return connection;
		} catch (SQLException e) {
			throw new FatalErrorException("Database connection failed", e);
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
