package org.skytemple.altaria.db;

import org.skytemple.altaria.exceptions.DbOperationException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class used to run a {@link java.sql.PreparedStatement} by chaining method calls, setting the value of the parameters
 * in order.
 */
public class PreparedStatementBuilder {
	private final Database db;
	private PreparedStatement statement;
	// String containing the SQL statement used to build the prepared statement
	private final String sqlStatement;
	// Index of the next parameter to set
	private int nextParamIndex;

	public PreparedStatementBuilder(Database db, String sqlStatement) throws DbOperationException {
		this.db = db;
		this.sqlStatement = sqlStatement;
		nextParamIndex = 1;
		db.runWithReconnect((connection) -> statement = connection.prepareStatement(sqlStatement),
			"Prepare " + sqlStatement);
	}

	public PreparedStatementBuilder setString(String value) throws DbOperationException {
		db.runWithReconnect((connection) -> statement.setString(nextParamIndex, value), "Set string: " + value);
		nextParamIndex++;
		return this;
	}

	public PreparedStatementBuilder setInt(Integer value) throws DbOperationException {
		db.runWithReconnect((connection) -> statement.setInt(nextParamIndex, value), "Set int: " + value);
		nextParamIndex++;
		return this;
	}

	public ResultSet executeQuery() throws DbOperationException {
		AtomicReference<ResultSet> result = new AtomicReference<>();
		db.runWithReconnect((connection) -> result.set(statement.executeQuery()), sqlStatement);
		return result.get();
	}

	public int executeUpdate() throws DbOperationException {
		AtomicInteger result = new AtomicInteger();
		db.runWithReconnect((connection) -> result.set(statement.executeUpdate()), sqlStatement);
		return result.get();
	}
}
