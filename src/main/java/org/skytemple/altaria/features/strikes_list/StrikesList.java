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

package org.skytemple.altaria.features.strikes_list;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.skytemple.altaria.definitions.CommandCreator;
import org.skytemple.altaria.definitions.senders.DelayedInteractionMsgSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;

/**
 * Class used to find out how many strikes each SkyTemple user has and build a list
 */
public class StrikesList {
	private final DiscordApi api;

	public StrikesList(CommandCreator commandCreator) {
		api = ApiGetter.get();

		commandCreator.registerCommand(
			SlashCommand.with("strikeslist", "Get how many strikes each user has").setDefaultDisabled()
		);

		api.addSlashCommandCreateListener(this::handleCommand);
	}

	private void handleCommand(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();
		String[] command = interaction.getFullCommandName().split(" ");

		if (command[0].equals("strikeslist")) {
			DelayedInteractionMsgSender sender = new DelayedInteractionMsgSender(interaction, false);
			new StrikeslistCommand(sender, sender).run();
		}
	}
}
