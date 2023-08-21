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
			resultSender.send("**" + user.getName() + "** has " + amount + " Guild Point(s)");
		} catch (DbOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
		}
	}
}
