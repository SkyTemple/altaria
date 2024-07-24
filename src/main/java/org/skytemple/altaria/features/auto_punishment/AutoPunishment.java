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

package org.skytemple.altaria.features.auto_punishment;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.skytemple.altaria.definitions.CommandArgumentList;
import org.skytemple.altaria.definitions.CommandCreator;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.Punishment;
import org.skytemple.altaria.definitions.db.AutoPunishmentDB;
import org.skytemple.altaria.definitions.db.Database;
import org.skytemple.altaria.definitions.enums.PunishmentAction;
import org.skytemple.altaria.definitions.exceptions.AsyncOperationException;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.ChannelMsgSender;
import org.skytemple.altaria.definitions.senders.ImmediateInteractionMsgSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.utils.DurationParser;
import org.skytemple.altaria.utils.Utils;

import java.time.Duration;
import java.util.Arrays;

/**
 * Used to automatically punish members when they receive a Vortex strike. This is helpful to overcome Vortex's
 * shortcomings, such as not being able to time-out members or tempban whithout automatically deleting messages.
 */
public class AutoPunishment {
	private static final long VORTEX_ID = 240254129333731328L;

	private final DiscordApi api;
	private final ExtConfig extConfig;
	private final AutoPunishmentDB db;
	private final Logger logger;

	public AutoPunishment(Database db, CommandCreator commandCreator) {
		api = ApiGetter.get();
		extConfig = ExtConfig.get();
		this.db = new AutoPunishmentDB(db);
		logger = Utils.getLogger(getClass());

		if (extConfig.strikeTimeoutsEnabled()) {
			// Register commands
			commandCreator.registerCommand(
				SlashCommand.with("punishment", "Set punishment to issue when a user reaches a given amount of strikes",
				Arrays.asList(
					SlashCommandOption.create(SlashCommandOptionType.LONG, "strikes", "Number of strikes", true),
					SlashCommandOption.create(SlashCommandOptionType.STRING, "action", "Action to perform (\"none\", " +
						"\"kick\", \"mute\" or \"ban\")", true),
					SlashCommandOption.create(SlashCommandOptionType.STRING, "duration", "Punishment duration. Format: " +
						"<time><s/m/h/d> (eg: 10m).", false)
				))
				.setDefaultDisabled()
			);

			commandCreator.registerCommand(
				SlashCommand.with("punishments", "Check punishments issued when a user reaches a given amount of strikes")
				.setDefaultDisabled()
			);

			// Create listeners
			api.addSlashCommandCreateListener(this::handleAutoPunishmentCommand);
			api.addMessageCreateListener(this::handleMsgEvent);
		}
	}

	private void handleAutoPunishmentCommand(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();
		String[] command = interaction.getFullCommandName().split(" ");
		ImmediateInteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
		CommandArgumentList arguments = new CommandArgumentList(interaction, sender);

		if (command[0].equals("punishment")) {
			int strikes = arguments.getInteger("strikes", true);
			String actionStr = arguments.getString("action", true);
			String durationStr = arguments.getString("duration", false);
			if (arguments.success()) {
				PunishmentAction action;
				Duration duration;
				try {
					action = PunishmentAction.valueOf(actionStr.toUpperCase());
				} catch (IllegalArgumentException e) {
					sender.send("Error: Invalid action provided");
					return;
				}
				if (action == PunishmentAction.BAN && durationStr != null) {
					sender.send("Error: It's impossible to implement tempbans right now. Use Vortex's strike system " +
						"for that.");
					return;
				}
				if (durationStr == null) {
					if (action.durationRequired()) {
						sender.send("Error: A duration must be specified for this type of punishment.");
						return;
					} else {
						duration = null;
					}
				} else {
					if (action.canHaveDuration()) {
						try {
							duration = DurationParser.parse(durationStr);
						} catch (IllegalArgumentException e) {
							sender.send("Error: Invalid duration provided");
							return;
						}
					} else {
						duration = null;
					}
				}
				new PunishmentCommand(db, strikes, action, duration, sender, sender).run();
			}
		} else if (command[0].equals("punishments")) {
			new PunishmentsCommand(db, sender, sender).run();
		}
	}

	private void handleMsgEvent(MessageCreateEvent event) {
		Message strikeMessage = event.getMessage();
		String messageStr = strikeMessage.getContent();
		ChannelMsgSender sender = new ChannelMsgSender(event.getChannel().getId());

		if (event.getMessageAuthor().getId() == VORTEX_ID &&
			event.getChannel().getId() == extConfig.getStrikeLogChannelId()) {
			VortexStrikeParser.Strike strike = VortexStrikeParser.parse(messageStr);
			if (strike != null) {
				Punishment punishment;
				try {
					punishment = db.get(strike.newNumStrikes());
				} catch (DbOperationException e) {
					new ErrorHandler(e).printToErrorChannel().run();
					return;
				}
				@SuppressWarnings("UnnecessaryUnicodeEscape") // Doesn't get displayed properly otherwise
				String reasonMsg = "[" + strike.oldNumStrikes() + " \u2192 " + strike.newNumStrikes() + " strikes]: " +
					strike.reason();
				Server server = event.getServer().orElse(null);
				if (server != null) {
					try {
						punishment.apply(server, strike.user(), reasonMsg);
					} catch (AsyncOperationException e) {
						sender.replyTo(strikeMessage);
						new ErrorHandler(e).sendMessage("Error while trying to apply strike punishment. Please " +
							"apply the punishment manually (" + punishment + ").", sender).printToErrorChannel().run();
					}
				} else {
					logger.error("Cannot get server associated to strike message. Message ID: " + event.getMessageId());
				}
			}
		}
	}
}
