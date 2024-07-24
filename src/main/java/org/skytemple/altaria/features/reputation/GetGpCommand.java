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

package org.skytemple.altaria.features.reputation;

import org.javacord.api.entity.user.User;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.MessageSender;

public class GetGpCommand implements Command {
	protected ReputationDB rdb;
	protected User user;
	protected MessageSender resultSender;
	protected MessageSender errorSender;

	/**
	 * Gets the amount of GP a user has
	 * @param rdb Reputation database instance
	 * @param user User whose GP will be checked
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public GetGpCommand(ReputationDB rdb, User user, MessageSender resultSender, MessageSender errorSender) {
		this.rdb = rdb;
		this.user = user;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
	}

	@Override
	public void run() {
		try {
			int amount = rdb.getPoints(user.getId());
			resultSender.send("**" + user.getName() + "** has " + amount + " Guild Point(s).");
		} catch (DbOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
		}
	}
}
