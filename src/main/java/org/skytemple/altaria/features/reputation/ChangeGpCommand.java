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

			String msg;
			if (amount >= 0) {
				msg = "Gave " + amount + " Guild Point(s) to ";
			} else {
				msg = "Took " + amount * -1 + " Guild Point(s) from ";
			}
			resultSender.send(msg + "**" + user.getName() + "** (current: " + rdb.getPoints(user.getId()) + ")");
		} catch (DbOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
		}
	}
}
