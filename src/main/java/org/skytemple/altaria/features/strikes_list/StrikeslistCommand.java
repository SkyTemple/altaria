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
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.senders.MessageSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.definitions.vortex.VortexPunishmentStrikeParser;
import org.skytemple.altaria.utils.JavacordUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.skytemple.altaria.definitions.Constants.VORTEX_ID;

public class StrikeslistCommand implements Command {
	protected MessageSender resultSender;
	protected MessageSender errorSender;
	private final DiscordApi api;
	private final ExtConfig extConfig;

	/**
	 * Creates a list that shows how many strikes each user has and prints it.
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public StrikeslistCommand(MessageSender resultSender, MessageSender errorSender) {
		this.resultSender = resultSender;
		this.errorSender = errorSender;

		api = ApiGetter.get();
		extConfig = ExtConfig.get();
	}

	@Override
	public void run() {
		// Maps user ID to strike details
		Map<Long, VortexPunishmentStrikeParser.StrikePunishment> result = new TreeMap<>();
		// Set containing the IDs of all banned users
		Set<Long> bannedUsers;
		try {
			bannedUsers = JavacordUtils.getBannedUsersIDs();
		} catch (ExecutionException | InterruptedException e) {
			new ErrorHandler(e).sendMessage("Error retrieving ban list.", errorSender).printToErrorChannel().run();
			return;
		}

		ServerTextChannel strikeLogChannel = api.getServerTextChannelById(extConfig.getStrikeLogChannelId()).orElse(null);
		if (strikeLogChannel == null) {
			errorSender.send("Error: Cannot find strike log channel.");
			return;
		}

		// Retrieve the current amount of strikes for each user
		for (Object o : strikeLogChannel.getMessagesAsStream().toArray()) {
			Message message = (Message) o;
			if (message.getAuthor().getId() != VORTEX_ID) {
				// Not a Vortex message, skip
				continue;
			}
			String text = message.getContent();

			VortexPunishmentStrikeParser.StrikePunishment strikePunishment = VortexPunishmentStrikeParser.parse(text);
			if (strikePunishment == null) {
				// Not a strike message, skip
				continue;
			}

			if (result.containsKey(strikePunishment.userId())) {
				// If we already have an entry for the user, skip it, since the most recent one will always have
				// the most up-to-date strike count.
				continue;
			}

			if (strikePunishment.moderator().equals("Altaria")) {
				// Altaria's punishments always mirror a Vortex one, ignore them
				continue;
			}

			result.put(strikePunishment.userId(), strikePunishment);
		}

		// Filter the list
		for (Iterator<Map.Entry<Long, VortexPunishmentStrikeParser.StrikePunishment>> it = result.entrySet().iterator();
			 it.hasNext();) {
			Map.Entry<Long, VortexPunishmentStrikeParser.StrikePunishment> entry = it.next();
			VortexPunishmentStrikeParser.StrikePunishment strikePunishment = entry.getValue();
			if (strikePunishment.newNumStrikes() == 0) {
				it.remove();
			} else if (bannedUsers.contains(strikePunishment.userId())) {
				// User was already banned, no point in counting their strikes
				it.remove();
			}
		}

		printResult(result);
	}

	private void printResult(Map<Long, VortexPunishmentStrikeParser.StrikePunishment> result) {
		StringBuilder textBuilder = new StringBuilder();
		StringBuilder csvBuilder = new StringBuilder();
		for (VortexPunishmentStrikeParser.StrikePunishment strikePunishment : result.values()) {
			textBuilder.append(strikePunishment.username())
				.append(" (ID: ")
				.append(strikePunishment.userId())
				.append("): ")
				.append(strikePunishment.newNumStrikes())
				.append("\n");
			csvBuilder.append(strikePunishment.userId())
				.append(",")
				.append(strikePunishment.newNumStrikes())
				.append("\n");
		}

		byte[] bytesText = textBuilder.toString().getBytes(StandardCharsets.UTF_8);
		byte[] bytesCsv = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
		resultSender.addAttachment(bytesText, "Strikes.txt")
			.addAttachment(bytesCsv, "Strikes.csv")
			.send();
	}
}
