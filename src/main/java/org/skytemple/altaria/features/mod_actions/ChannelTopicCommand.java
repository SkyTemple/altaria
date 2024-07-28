/*
 * Copyright (c) 2024. Frostbyte and other contributors.
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
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.Constants;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChannelTopicCommand implements Command {
	protected User user;
	protected Channel channel;
	protected String newTopic;
	protected MessageSender resultSender;
	protected MessageSender errorSender;

	/**
	 * Changes the topic of the specified channel
	 * @param user User who run the command
	 * @param channel Channel whose topic will be changed
	 * @param newTopic New topic of the channel
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public ChannelTopicCommand(User user, Channel channel, String newTopic, MessageSender resultSender,
		MessageSender errorSender) {
		this.user = user;
		this.channel = channel;
		this.newTopic = newTopic;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
	}

	@Override
	public void run() {
		channel.asServerTextChannel().ifPresentOrElse(serverTextChannel -> {
			try {
				serverTextChannel.createUpdater().setTopic(newTopic)
					.setAuditLogReason("Command run by " + user.getName())
					.update().get(Constants.ACTION_TIMEOUT, TimeUnit.SECONDS);
				resultSender.send("Channel topic successfully changed.");
			} catch (CompletionException | InterruptedException | ExecutionException e) {
				new ErrorHandler(e).sendMessage("Error: Couldn't change channel topic.", errorSender)
					.printToErrorChannel().run();
			} catch (TimeoutException e) {
				errorSender.send("Error: Rename operation timed out. The bot might be getting rate-limited.");
			}
		}, () -> errorSender.send("Error: Command must be used on a text channel."));
	}
}
