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

package org.skytemple.altaria.features.mod_actions;

import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.user.User;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.Constants;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.senders.InteractionMsgSender;

import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.skytemple.altaria.definitions.Constants.MAX_SLOWMODE_TIME;

public class SlowmodeCommand implements Command {
	protected User user;
	protected Channel channel;
	protected Duration time;
	protected InteractionMsgSender sender;

	/**
	 * Sets the slowmode time of the specified channel
	 * @param user User who run the command
	 * @param channel Channel whose slowmode will be modified
	 * @param time Time to set the slowmode timer to
	 * @param sender Used to send result and error messages to the user. Must be an interaction since both the
	 *               result message and error messages are ephemeral.
	 */
	public SlowmodeCommand(User user, Channel channel, Duration time, InteractionMsgSender sender) {
		this.user = user;
		this.channel = channel;
		this.time = time;
		this.sender = sender;
		sender.setEphemeral();
	}

	@Override
	public void run() {
		channel.asServerTextChannel().ifPresentOrElse(serverChannel -> {
			int timeSeconds = (int) time.toSeconds();
			if (timeSeconds > MAX_SLOWMODE_TIME) {
				sender.send("Error: Maximum slowmode time is " + (MAX_SLOWMODE_TIME / 3600) + " hours.");
			} else {
				try {
					serverChannel.createUpdater().setSlowmodeDelayInSeconds(timeSeconds)
						.setAuditLogReason("Command run by " + user.getName())
						.update().get(Constants.ACTION_TIMEOUT, TimeUnit.SECONDS);
					sender.send("Slowmode successfully updated to " + timeSeconds + " seconds.");
				} catch (CompletionException | InterruptedException | ExecutionException e) {
					new ErrorHandler(e).sendMessage("Error: Couldn't set slowmode time.", sender)
						.printToErrorChannel().run();
				} catch (TimeoutException e) {
					sender.send("Error: Channel update operation timed out. The bot might be getting rate-limited.");
				}
			}
		}, () -> sender.send("Error: Channel is not a text channel."));
	}
}
