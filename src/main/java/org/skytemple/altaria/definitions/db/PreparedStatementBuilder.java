/*
 * Copyright (c) 2023-2024. End45 and other contributors.
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
		/*
			If the connection has been lost, this operation won't cause an error, but an exception will be thrown when
			trying to execute the statement later because the reference to the statement will be outdated.
			To avoid this, we check if a reconnection is necessary now.
		 */
		db.ensureConnection();
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

	public PreparedStatementBuilder setLong(Long value) throws DbOperationException {
		db.runWithReconnect((connection) -> statement.setLong(nextParamIndex, value), "Set long: " + value);
		nextParamIndex++;
		return this;
	}

	/**
	 * Sets a parameter value to null
	 * @param sqlColumnType Column type, as defined in {@link java.sql.Types}.
	 * @return this
	 */
	public PreparedStatementBuilder setNull(int sqlColumnType) throws DbOperationException {
		db.runWithReconnect((connection) -> statement.setNull(nextParamIndex, sqlColumnType), "Set null");
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
