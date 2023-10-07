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

import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.MultiGpCollection;
import org.skytemple.altaria.definitions.MultiGpList;
import org.skytemple.altaria.definitions.senders.InteractionMsgSender;

import static org.skytemple.altaria.features.reputation.Reputation.COMPONENT_LIST_GP_CLEAR;
import static org.skytemple.altaria.features.reputation.Reputation.COMPONENT_LIST_GP_CONFIRM;

public class MultiGpListCommand implements Command {
	private final MultiGpCollection multiGpCollection;
	private final long userId;
	private final InteractionMsgSender resultSender;

	/**
	 * Displays the multi-GP list of the specified user, alongside a message to confirm or clear the list.
	 * @param multiGpCollection Collection that contains all the multi-gp lists
	 * @param userId ID of the user whose list should be displayed
	 * @param resultSender Used to send result messages to the user. Must be an interaction since the result message
	 *                     is ephemeral
	 */
	public MultiGpListCommand(MultiGpCollection multiGpCollection, long userId, InteractionMsgSender resultSender) {
		this.multiGpCollection = multiGpCollection;
		this.userId = userId;
		this.resultSender = resultSender;
		resultSender.setEphemeral();
	}

	@Override
	public void run() {
		MultiGpList gpList = multiGpCollection.get(userId);
		if (gpList == null) {
			resultSender.send("The multi-GP list is empty!");
		} else {
			resultSender.addEmbed(gpList.toEmbed(true));
			resultSender.addComponent(ActionRow.of(
				Button.success(COMPONENT_LIST_GP_CONFIRM, "Confirm"),
				Button.danger(COMPONENT_LIST_GP_CLEAR, "Clear all")
			));
			resultSender.send();
		}
	}
}
