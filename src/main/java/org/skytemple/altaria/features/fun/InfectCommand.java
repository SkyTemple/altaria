/*
 * Copyright (c) 2023-2025. Frostbyte and other contributors.
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

package org.skytemple.altaria.features.fun;

import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.Constants;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.senders.MessageSender;
import org.skytemple.altaria.definitions.singletons.ExtConfig;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class InfectCommand implements Command {
	// True if the command was successfully run, false if it wasn't, null if it wasn't run yet.
	public Boolean success;

	protected Server server;
	protected User user;
	protected User target;
	protected MessageSender resultSender;
	protected MessageSender errorSender;

	private final ExtConfig extConfig;

	/**
	 * Gives the set Fun 2025 role to a user and posts a message in the Fun 2025 channel announcing it
	 * @param server Server where the command is run
	 * @param user User who ran the command
	 * @param target User to give the role to
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public InfectCommand(Server server, User user, User target, MessageSender resultSender, MessageSender errorSender) {
		this.server = server;
		this.user = user;
		this.target = target;
		this.resultSender = resultSender;
		this.errorSender = errorSender;

		extConfig = ExtConfig.get();

		success = null;
	}

	@Override
	public void run() {
		Role role = server.getRoleById(extConfig.getFun2025RoleId()).orElse(null);
		Channel channel = server.getChannelById(extConfig.getFun2025ChannelId()).orElse(null);

		success = false;

		if (role == null) {
			errorSender.send("Error: Fun2025 role not found");
			return;
		}

		if (channel == null) {
			errorSender.send("Error: Fun2025 channel not found");
			return;
		}

		TextChannel textChannel = channel.asTextChannel().orElse(null);
		if (textChannel == null) {
			errorSender.send("Error: Fun2025 channel is not a text channel");
			return;
		}

		if (target.getRoles(server).contains(role)) {
			resultSender.send(target.getDisplayName(server) + " is already infected!");
		} else {
			try {
				target.addRole(role).get(Constants.ACTION_TIMEOUT, TimeUnit.SECONDS);
				resultSender.send("**" + target.getDisplayName(server) + "** was infected! How careless!");
			} catch (CompletionException | InterruptedException | ExecutionException e) {
				new ErrorHandler(e).sendMessage("Error: Could not give role to target user.", errorSender)
					.printToErrorChannel().run();
				return;
			} catch (TimeoutException e) {
				errorSender.send("Error: Role update operation timed out. The bot might be getting rate-limited.");
				return;
			}

			success = true;

			try {
				textChannel.sendMessage("**" + user.getDisplayName(server) + "** has infected **" +
					target.getDisplayName(server) + "**!").get(Constants.ACTION_TIMEOUT, TimeUnit.SECONDS);
			} catch (CompletionException | InterruptedException | ExecutionException | TimeoutException e) {
				// No big deal if the message cannot be posted, no need to warn the user about it.
				new ErrorHandler(e).printToErrorChannel().run();
			}
		}
	}
}
