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

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.interaction.MessageContextMenuCommandEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.*;
import org.skytemple.altaria.definitions.CommandArgumentList;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.senders.ImmediateInteractionMsgSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;

import java.util.Arrays;

/**
 * Allows performing certain mod actions through bot commands. This can be used to avoid granting a permission that
 * might include other undesired sub-permissions.
 */
public class ModActions {
	private static final String PIN_CONTEXT_ACTION = "Pin/Unpin message";

	private final DiscordApi api;
	private final ExtConfig extConfig;

	public ModActions() {
		api = ApiGetter.get();
		extConfig = ExtConfig.get();

		// Register commands
		SlashCommand.with("renamechannel", "Rename a channel", Arrays.asList(
			SlashCommandOption.create(SlashCommandOptionType.STRING, "name", "New channel name", true),
			SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "channel", "Channel to rename. Omit to rename the " +
				"current channel.", false)
		))
		.setDefaultDisabled()
		.createForServer(api, extConfig.getGuildId())
		.exceptionally(e -> {new ErrorHandler(e).printToErrorChannel().run(); return null;})
		.join();

		MessageContextMenu.with(PIN_CONTEXT_ACTION)
			.setDefaultDisabled()
			.createForServer(api, extConfig.getGuildId())
			.exceptionally(e -> {new ErrorHandler(e).printToErrorChannel().run(); return null;})
			.join();

		// Create listeners
		api.addSlashCommandCreateListener(this::handleModActionCommand);
		api.addMessageContextMenuCommandListener(this::handleContextAction);
	}

	private void handleModActionCommand(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();
		String[] command = interaction.getFullCommandName().split(" ");
		ImmediateInteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
		CommandArgumentList arguments = new CommandArgumentList(interaction, sender);

		if (command[0].equals("renamechannel")) {
			String name = arguments.getString("name", true);
			Channel channel = arguments.getChannel("channel", false);
			if (arguments.success()) {
				if (channel == null) {
					channel = interaction.getChannel().orElse(null);
				}
				if (channel != null) {
					new RenameChannelCommand(interaction.getUser(), channel, name, sender, sender).run();
				} else {
					sender.send("Error trying to retrieve channel ID.");
				}
			}
		}
	}

	private void handleContextAction(MessageContextMenuCommandEvent event) {
		MessageContextMenuInteraction interaction = event.getMessageContextMenuInteraction();
		ImmediateInteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
		String cmdName = interaction.getCommandName();
		if (cmdName.equals(PIN_CONTEXT_ACTION)) {
			Message message = interaction.getTarget();
			new PinMessageCommand(message, sender, sender).run();
		}
	}
}
