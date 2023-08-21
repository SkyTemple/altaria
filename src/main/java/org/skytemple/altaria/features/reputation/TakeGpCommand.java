package org.skytemple.altaria.features.reputation;

import org.javacord.api.entity.user.User;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.senders.MessageSender;

public class TakeGpCommand extends ChangeGpCommand {
	/**
	 * Takes GP from a user
	 * @param rdb Reputation database instance
	 * @param user User to take GP from
	 * @param amount Amount of GP to take. Must be greater than 0.
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public TakeGpCommand(ReputationDB rdb, User user, int amount, MessageSender resultSender, MessageSender errorSender) {
		super(rdb, user, amount, resultSender, errorSender);
	}

	@Override
	public void run() {
		if (amount > 0) {
			amount *= -1;
			super.run();
		} else {
			errorSender.send("Error: The amount of points must be > 0");
		}
	}
}
