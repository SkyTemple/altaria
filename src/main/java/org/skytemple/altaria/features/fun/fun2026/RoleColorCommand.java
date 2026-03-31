/*
 * Copyright (c) 2023-2026. Frostbyte and other contributors.
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

package org.skytemple.altaria.features.fun.fun2026;

import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.skytemple.altaria.definitions.ButtonActionList;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.InteractionMsgSender;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.awt.*;

public class RoleColorCommand implements Command {
	private final ReputationDB rdb;
	private final User user;
	private final User selfUser;
	private final Role roleToUpdate;
	private final Color color;
	private final Role roleToCopyFrom;
	private final int displayCost;
	private final ButtonActionList<Fun2026.RecolorButtonAction> actionList;
	private final InteractionMsgSender resultSender;
	private final MessageSender errorSender;

	/**
	 * Sends a message to the user asking them to confirm a role recolor operation, with a button to do so.
	 * <p>
	 * If the user does not have enough GP to perform the operation, displays a message stating this instead.
	 *
	 * @param rdb Reputation database instance
	 * @param user User who ran the command
	 * @param selfUser User object corresponding to the bot
	 * @param roleToUpdate Role whose color should be updated
	 * @param color New role color. Cannot be null. #000000 to clear the role color.
	 * @param roleToCopyFrom Role the color is being copied from
	 * @param displayCost Cost to display to the user. The final cost might end up being different if the user takes
	 * too long to confirm the operation, since it changes over time.
	 * @param actionList List where the action to perform will be stored
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public RoleColorCommand(ReputationDB rdb, User user, User selfUser, Role roleToUpdate, Color color, Role roleToCopyFrom,
		int displayCost, ButtonActionList<Fun2026.RecolorButtonAction> actionList, InteractionMsgSender resultSender,
		MessageSender errorSender) {
		if (color == null) {
			throw new IllegalArgumentException("Color cannot be null");
		}

		this.rdb = rdb;
		this.user = user;
		this.selfUser = selfUser;
		this.roleToUpdate = roleToUpdate;
		this.color = color;
		this.roleToCopyFrom = roleToCopyFrom;
		this.displayCost = displayCost;
		this.actionList = actionList;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
	}

	@Override
	public void run() {
		int userGP;
		try {
			userGP = rdb.getPointsInt(user.getId());
		} catch (DbOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
			return;
		}

		if (userGP < displayCost) {
			resultSender.setEphemeral()
				.send("Recoloring the **" + roleToUpdate.getName() + "** role currently requires " + displayCost +
				" GP, but you only have " + userGP + ".\n" +
				"Cost will decrease over time if no one recolors the role, so try again later!");
			return;
		}

		if (!selfUser.canManageRole(roleToUpdate)) {
			resultSender.setEphemeral().send("I can't make changes to the **" + roleToUpdate.getName() + "** role.");
			return;
		}

		Fun2026.RecolorButtonAction recolorAction = new Fun2026.RecolorButtonAction(roleToUpdate, color, roleToCopyFrom);
		String buttonId = actionList.storeAction(recolorAction);

		Button button = Button.primary(buttonId, "Recolor role");

		resultSender.setEphemeral()
			.setText(getMessageText())
			.addComponent(ActionRow.of(button))
			.send();
	}

	private String getMessageText() {
		if (color.equals(new Color(0, 0, 0))) {
			return "The color of the **" + roleToUpdate.getName() + "** role will be cleared.\n" +
				"This currently has a cost of " + displayCost + " GP. The cost might increase if someone else " +
				"recolors the role before you do.\n" +
				"Press the button below to confirm.";
		}

		String colorHex = String.format("#%06X", color.getRGB() & 0xFFFFFF);

		if (roleToCopyFrom == null) {
			return "The color of the **" + roleToUpdate.getName() + "** role will be set to " + colorHex + ".\n" +
				"This currently has a cost of " + displayCost + " GP. The cost might increase if someone else " +
				"recolors the role before you do.\n" +
				"Press the button below to confirm.";
		} else {
			return  "The color of the **" + roleToUpdate.getName() + "** role will be changed to match the " +
				"color of the **" + roleToCopyFrom.getName() + "** role (" + colorHex + ").\n" +
				"This currently has a cost of " + displayCost + " GP. The cost might increase if someone else " +
				"recolors the role before you do.\n" +
				"Press the button below to confirm.";
		}
	}
}
