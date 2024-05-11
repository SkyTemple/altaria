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

package org.skytemple.altaria.features.mod_actions;

import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.user.User;
import org.skytemple.altaria.definitions.senders.MessageSender;

public class RenameThreadCommand extends RenameChannelCommand {

	/**
	 * Renames the specified thread
	 * @param user User who run the command
	 * @param thread Thread that will be renamed. If it's not a thread, the command will fail when run.
	 * @param newName New name of the thread
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	public RenameThreadCommand(User user, Channel thread, String newName, MessageSender resultSender,
		MessageSender errorSender) {
		super(user, thread, newName, resultSender, errorSender);
		allowThreadsOnly = true;
	}
}
