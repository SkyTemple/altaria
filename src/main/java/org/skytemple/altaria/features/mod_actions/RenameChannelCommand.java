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

package org.skytemple.altaria.features.mod_actions;

import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.user.User;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.Constants;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.util.concurrent.*;

import static org.skytemple.altaria.utils.JavacordUtils.updateThreadName;

public class RenameChannelCommand implements Command {
	protected User user;
	protected Channel channel;
	protected String newName;
	protected MessageSender resultSender;
	protected MessageSender errorSender;

	// If true, the command can only rename threads, not channels.
	protected boolean allowThreadsOnly;

	/**
	 * Renames the specified channel
	 * @param user User who run the command
	 * @param channel Channel that will be renamed
	 * @param newName New name of the channel
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public RenameChannelCommand(User user, Channel channel, String newName, MessageSender resultSender,
		MessageSender errorSender) {
		this.user = user;
		this.channel = channel;
		this.newName = newName;
		this.resultSender = resultSender;
		this.errorSender = errorSender;

		allowThreadsOnly = false;
	}

	@Override
	public void run() {
		channel.asServerChannel().ifPresentOrElse(serverChannel -> {
			try {
				ServerThreadChannel thread = serverChannel.asServerThreadChannel().orElse(null);
				if (thread != null) {
					updateThreadName(thread, newName, "Command run by " + user.getName()).get(
						Constants.ACTION_TIMEOUT, TimeUnit.SECONDS);
					resultSender.send("Thread successfully renamed to **" + newName + "**.");
				} else {
					if (allowThreadsOnly) {
						errorSender.send("Error: This command can only be used to rename threads.");
					} else {
						serverChannel.createUpdater().setName(newName).setAuditLogReason("Command run by " + user.getName())
							.update().get(Constants.ACTION_TIMEOUT, TimeUnit.SECONDS);
						resultSender.send("Channel successfully renamed to **" + newName + "**.");
					}
				}
			} catch (CompletionException | InterruptedException | ExecutionException e) {
				new ErrorHandler(e).sendMessage("Error: Couldn't rename channel.", errorSender).printToErrorChannel().run();
			} catch (TimeoutException e) {
				errorSender.send("Error: Rename operation timed out. The bot might be getting rate-limited.");
			}
		}, () -> errorSender.send("Error: Only server channels can be renamed."));
	}
}
