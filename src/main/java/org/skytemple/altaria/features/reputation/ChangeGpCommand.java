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

package org.skytemple.altaria.features.reputation;

import org.javacord.api.entity.user.User;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.MessageSender;

public class ChangeGpCommand implements Command {
	protected ReputationDB rdb;
	protected User user;
	protected int amount;
	protected MessageSender resultSender;
	protected MessageSender errorSender;

	/**
	 * Modifies the GP count of a user
	 * @param rdb Reputation database instance
	 * @param user User whose GP will be modified
	 * @param amount Amount of GP to give (if > 0) or take (if < 0)
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public ChangeGpCommand(ReputationDB rdb, User user, int amount, MessageSender resultSender, MessageSender errorSender) {
		this.rdb = rdb;
		this.user = user;
		this.amount = amount;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
	}

	@Override
	public void run() {
		try {
			rdb.addPoints(user.getId(), amount);
			sendResultMessage(resultSender);
		} catch (DbOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
		}
	}

	/**
	 * Sends a public result message showing how many points the user got or lost.
	 * @param sender Used to send the result message
	 */
	protected void sendResultMessage(MessageSender sender) throws DbOperationException {
		String msg;
		if (amount >= 0) {
			msg = "Gave " + amount + " Guild Point(s) to ";
		} else {
			msg = "Took " + amount * -1 + " Guild Point(s) from ";
		}
		sender.send(msg + "**" + user.getName() + "** (current: " + rdb.getPoints(user.getId()) + ").");
	}
}
