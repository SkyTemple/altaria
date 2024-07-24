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
			errorSender.send("Error: The amount of points must be > 0.");
		}
	}
}
