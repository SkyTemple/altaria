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
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.MultiGpCollection;
import org.skytemple.altaria.definitions.MultiGpList;
import org.skytemple.altaria.definitions.senders.InteractionMsgSender;

public class MultiGpClearCommand implements Command {
	private final MultiGpCollection multiGpCollection;
	private final long cmdUserId;
	private final User user;
	private final InteractionMsgSender resultSender;

	/**
	 * Removes a certain user from the multi-GP list of the user who ran the command
	 * @param multiGpCollection Collection where the data will be removed
	 * @param user User to remove
	 * @param cmdUserId ID of the user who ran the command. The data will be added to their entry in the multi-GP
	 *                  collection.
	 * @param resultSender Used to send result messages to the user. Must be an interaction since the result message
	 *                     is ephemeral
	 */
	public MultiGpClearCommand(MultiGpCollection multiGpCollection, User user, long cmdUserId,
		InteractionMsgSender resultSender) {
		this.multiGpCollection = multiGpCollection;
		this.user = user;
		this.cmdUserId = cmdUserId;
		this.resultSender = resultSender;
		resultSender.setEphemeral();
	}

	@Override
	public void run() {
		MultiGpList gpList = multiGpCollection.get(cmdUserId);
		if (gpList == null) {
			resultSender.send("The multi-GP list is empty!");
		} else {
			gpList.remove(user.getId());
			resultSender.send("**" + user.getName() + "** removed from the multi-GP list.");
		}
	}
}
