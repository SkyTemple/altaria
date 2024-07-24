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
import org.skytemple.altaria.utils.Utils;

public class ChangeGpCommand implements Command {
	// When displaying the result message from this command, the amount shown will be rounded to this
	// many decimal places.
	protected static final int RESULT_MSG_ROUND_DECIMALS = 4;

	protected ReputationDB rdb;
	protected User user;
	protected double amount;
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
	public ChangeGpCommand(ReputationDB rdb, User user, double amount, MessageSender resultSender, MessageSender errorSender) {
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
			msg = "Gave " + Utils.gpAmountToString(amount, RESULT_MSG_ROUND_DECIMALS) + " Guild Point(s) to ";
		} else {
			msg = "Took " + Utils.gpAmountToString(amount * -1, RESULT_MSG_ROUND_DECIMALS) + " Guild Point(s) from ";
		}
		sender.send(msg + "**" + user.getName() + "** (current: " + rdb.getPointsInt(user.getId()) + ").");
	}
}
