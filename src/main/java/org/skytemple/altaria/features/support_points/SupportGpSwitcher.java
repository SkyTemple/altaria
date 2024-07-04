/*
 * Copyright (c) 2024. End45 and other contributors.
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

package org.skytemple.altaria.features.support_points;

import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.db.SupportThreadsDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.InteractionMsgSender;

import java.util.Map;
import java.util.TreeMap;

/**
 * Class used to enable or disable support GP for a given user on a given thread
 */
public class SupportGpSwitcher {
	// Component IDs used for UI buttons. Can be used to identify which button the user pressed when an iteraction
	// arrives and act accordingly.
	public static final String COMPONENT_SUPPORT_GP_DISABLE = "supportGpDisable";
	public static final String COMPONENT_SUPPORT_GP_ENABLE = "supportGpEnable";

	private final SupportThreadsDB sdb;
	// Used to store the action to perform after the "enable/disable GP" confirmation button is clicked. One entry
	// for each user who ran the context menu action.
	private final Map<Long, SupportGpAction> supportGpActions;

	public SupportGpSwitcher(SupportThreadsDB sdb) {
		this.sdb = sdb;
		supportGpActions = new TreeMap<>();
	}

	/**
	 * Displays an ephemeral message with a menu to switch if the given user should receive GP for their contributions
	 * on the given thread.
	 * @param thread Thread where GP should be enabled or disabled
	 * @param userId ID of the user to enable or disable the GP for
	 * @param username Name of the user to enable or disable the GP for
	 * @param cmdUserId ID of the user who requested the GP switch menu
	 * @param resultSender Sender used to send the message. Must be an {@link InteractionMsgSender}, since the message
	 * will be ephemeral.
	 * @param errorSender Sender used to send error messages that happen while running the command. Must be an
	 * {@link InteractionMsgSender}, since the message will be ephemeral.
	 */
	public void showSupportGpSwitchMenu(ServerThreadChannel thread, long userId, String username,
		long cmdUserId, InteractionMsgSender resultSender, InteractionMsgSender errorSender) {
		try {
			long threadId = thread.getId();
			boolean isOp = thread.getOwnerId() == userId;
			boolean currentValue = sdb.shouldUserGetGP(userId, threadId, isOp);
			String currentValueStr = currentValue ? "**does receive**" : "**does not receive**";
			Button button;
			if (currentValue) {
				button = Button.danger(COMPONENT_SUPPORT_GP_DISABLE, "Disable support GP for this thread");
			} else {
				button = Button.success(COMPONENT_SUPPORT_GP_ENABLE, "Enable support GP for this thread");
			}

			// Save the parameters for later
			supportGpActions.put(cmdUserId, new SupportGpAction(userId, username, isOp, threadId, !currentValue));

			resultSender.setEphemeral()
				.setText(username + " currently " + currentValueStr + " GP for their contributions to " +
					"this thread. Click the button below to change it.")
				.addComponent(ActionRow.of(button))
				.send();
		} catch (DbOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
		}
	}

	/**
	 * Confirms a GP switch action previously initiated with
	 * {@link #showSupportGpSwitchMenu(ServerThreadChannel, long, String, long, InteractionMsgSender, InteractionMsgSender)}.
	 * @param cmdUserId ID of the user who requested the GP switch
	 * @param resultSender Sender used to send the message. Must be an {@link InteractionMsgSender}, since the message
	 * will be ephemeral.
	 * @param errorSender Sender used to send error messages that happen while running the command. Must be an
	 * {@link InteractionMsgSender}, since the message will be ephemeral.
	 * @return True if any kind of response was sent, false otherwise.
	 */
	public boolean confirmSupportGpSwitch(long cmdUserId, InteractionMsgSender resultSender,
		InteractionMsgSender errorSender) {
		try {
			SupportGpAction action = supportGpActions.getOrDefault(cmdUserId, null);
			if (action != null) {
				boolean currentValue = sdb.shouldUserGetGP(action.userId, action.threadId, action.isOp);
				if (action.enableGp == currentValue) {
					// The correct state is already set. Maybe someone else ran the command at the same time. Do nothing.
					return false;
				} else {
					try {
						sdb.setUserSupportGp(action.userId, action.threadId, action.enableGp, action.isOp);
						String actionStr = action.enableGp ? "now" : "no longer";

						resultSender.setEphemeral().setText(action.username + " will " + actionStr + " receive GP for " +
							"their messages on this thread.").send();
						supportGpActions.remove(cmdUserId);
						return true;
					} catch (DbOperationException e) {
						new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
						return true;
					}
				}
			} else {
				resultSender.setEphemeral()
					.setText("Error: No action to confirm. Run the context menu action first.").send();
				return true;
			}
		} catch (DbOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
			return true;
		}
	}

	/**
	 * Used to store an action to perform when the "enable/disable GP" confirmation button is clicked
	 * @param userId User ID
	 * @param username Name of the user
	 * @param isOp True if the user created the thread
	 * @param threadId Thread ID
	 * @param enableGp True if the user should get GP on the specified thread
	 */
	private record SupportGpAction(long userId, String username, boolean isOp, long threadId, boolean enableGp) {}
}
