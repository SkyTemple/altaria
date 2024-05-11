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

package org.skytemple.altaria.features.reputation;

import org.javacord.api.entity.user.User;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.MessageSender;
import org.skytemple.altaria.utils.DiscordUtils;

public class ChangeGpInterfaceCommand extends ChangeGpCommand {
	protected MessageSender publicResultSender;
	protected MessageSender privateResultSender;

	/**
	 * Modifies the GP count of a user and prints the result of the operation in JSON format.
	 * @param rdb Reputation database instance
	 * @param user User whose GP will be modified
	 * @param amount Amount of GP to give (if > 0) or take (if < 0)
	 * @param publicResultSender Used to send the message indicating that GP were given
	 * @param privateResultSender Used to send the message with the result of the command in JSON format
	 * @param errorSender Used to send error messages
	 */
	public ChangeGpInterfaceCommand(ReputationDB rdb, User user, int amount, MessageSender publicResultSender,
									MessageSender privateResultSender, MessageSender errorSender) {
		super(rdb, user, amount, null, errorSender);
		this.publicResultSender = publicResultSender;
		this.privateResultSender = privateResultSender;
	}

	@Override
	public void run() {
		try {
			rdb.addPoints(user.getId(), amount);
			sendResultMessage(publicResultSender);
			DiscordUtils.sendJsonResult(privateResultSender, true, "See channel");
		} catch (DbOperationException e) {
			DiscordUtils.sendJsonResult(privateResultSender, false, "Error trying to run the command");
			new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
		}
	}
}
