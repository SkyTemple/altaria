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

package org.skytemple.altaria.features.reputation;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;
import org.skytemple.altaria.definitions.CommandArgumentList;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.MultiGpCollection;
import org.skytemple.altaria.definitions.MultiGpList;
import org.skytemple.altaria.definitions.db.Database;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.ChannelMsgSender;
import org.skytemple.altaria.definitions.senders.ImmediateInteractionMsgSender;
import org.skytemple.altaria.definitions.senders.NullMsgSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.utils.DiscordUtils;

import java.util.Arrays;
import java.util.Collections;

/**
 * Class used to handle reputation commands and events
 */
public class Reputation {
	private static final long SPRITEBOT_ID = 548718661129732106L;
	private static final long SPRITEBOT_COMMANDS_CHANNEL_ID = 822865440489472020L;

	// Component IDs
	public static final String COMPONENT_LIST_GP_CONFIRM = "listGpConfirm";
	public static final String COMPONENT_LIST_GP_CLEAR = "listGpClear";

	private final DiscordApi api;
	private final ReputationDB rdb;
	private final ExtConfig extConfig;

	// Used to prevent duplicated queries to the database. Gets invalidated when a GP-changing command is run.
	private Leaderboard cachedLeaderboard;
	// Holds the multi-GP lists for the /multigp commands. The collection contains one list for each user that
	// used the command. Each multi-GP list maps a user to the amount of GP they will receive.
	private final MultiGpCollection multiGpCollection;

	public Reputation(Database db) {
		api = ApiGetter.get();
		rdb = new ReputationDB(db);
		extConfig = ExtConfig.get();
		cachedLeaderboard = null;
		multiGpCollection = new MultiGpCollection();

		// Register commands
		SlashCommand.with("gp", "Guild point management commands", Arrays.asList(
			SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "add", "Add points to a user",
				Arrays.asList(
					SlashCommandOption.create(SlashCommandOptionType.USER, "user", "User that will receive the GP", true),
					SlashCommandOption.create(SlashCommandOptionType.LONG, "amount", "Amount of GP to give (> 0)", true)
				)
			),
			SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "take", "Take points from a user",
				Arrays.asList(
					SlashCommandOption.create(SlashCommandOptionType.USER, "user", "User that will lose the GP", true),
					SlashCommandOption.create(SlashCommandOptionType.LONG, "amount", "Amount of GP to take (> 0)", true)
				)
			)
		))
		.setDefaultDisabled()
		.createForServer(api, extConfig.getGuildId())
		.exceptionally(e -> {new ErrorHandler(e).printToErrorChannel().run(); return null;})
		.join();

		SlashCommand.with("getgp", "Guild point user commands", Arrays.asList(
			SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "check", "Check the " +
				"amount of points a user has", Collections.singletonList(
					SlashCommandOption.create(SlashCommandOptionType.USER, "user", "User whose GP will be checked", true)
				)
			),
			SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "list", "View the full points " +
				"leaderboard, in descending order", Collections.singletonList(
					SlashCommandOption.create(SlashCommandOptionType.LONG, "page", "Page to retrieve. Use negative " +
						"numbers to retrieve a page starting from the end.", true)
				))
		))
		.createForServer(api, extConfig.getGuildId())
		.exceptionally(e -> {new ErrorHandler(e).printToErrorChannel().run(); return null;})
		.join();

		SlashCommand.with("multigp", "Commands to give GP to multiple users at once. Run /multigp list to confirm or " +
			"discard changes.", Arrays.asList(
			SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "add", "Add/Remove GP. The " +
				"operation will be added to the multi-GP list so it can be batch-executed later.", Arrays.asList(
					SlashCommandOption.create(SlashCommandOptionType.USER, "user", "User that will receive/lose the " +
					"GP", true),
					SlashCommandOption.create(SlashCommandOptionType.LONG, "amount", "Amount of GP to give/take", true)
				)
			),
			SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "clear", "Remove a user from the " +
				"multi-GP list",
				Collections.singletonList(
					SlashCommandOption.create(SlashCommandOptionType.USER, "user", "User tp remove", true)
				)
			),
			SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "list", "Shows the multi-GP list, " +
				"with options to confirm the commands or clear the list.")
		))
		.setDefaultDisabled()
		.createForServer(api, extConfig.getGuildId())
		.exceptionally(e -> {new ErrorHandler(e).printToErrorChannel().run(); return null;})
		.join();

		// Register listeners
		api.addSlashCommandCreateListener(this::handleGpCommand);
		api.addMessageComponentCreateListener(this::handleMessageComponent);
		if (extConfig.spritebotGpCommandsEnabled()) {
			api.addMessageCreateListener(this::handleBotGpCommand);
		}
	}

	private void handleGpCommand(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();
		String[] command = interaction.getFullCommandName().split(" ");
		ImmediateInteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
		CommandArgumentList arguments = new CommandArgumentList(interaction, sender);

		if (command[0].equals("gp")) {
			if (command[1].equals("add") || command[1].equals("take")) {
				User user = arguments.getCachedUser("user", true);
				Integer amount = arguments.getInteger("amount", true);
				if (arguments.success()) {
					if (command[1].equals("add")) {
						new GiveGpCommand(rdb, user, amount, sender, sender).run();
					} else {
						new TakeGpCommand(rdb, user, amount, sender, sender).run();
					}
					cachedLeaderboard = null;
				}
			} else {
				sender.send("Error: Unrecognized GP subcommand.");
			}
		} else if (command[0].equals("getgp")) {
			if (command[1].equals("check")) {
				User user = arguments.getCachedUser("user", true);
				if (arguments.success()) {
					new GetGpCommand(rdb, user, sender, sender).run();
				}
			} else if (command[1].equals("list")) {
				Integer page = arguments.getInteger("page", true);
				if (arguments.success()) {
					// Convert page to a 0-indexed value if positive
					page = page < 0 ? page : page - 1;
					if (cachedLeaderboard == null) {
						ListGpCommand listGp = new ListGpCommand(rdb, page, sender, sender);
						listGp.run();
						cachedLeaderboard = listGp.getLeaderboard();
					} else {
						new ListGpCommand(cachedLeaderboard, page, sender, sender).run();
					}
				}
			} else {
				sender.send("Error: Unrecognized GP subcommand.");
			}
		} else if (command[0].equals("multigp")) {
			// Maybe these should be moved to command classes too

			long cmdUserId = interaction.getUser().getId();

			if (command[1].equals("add")) {
				User user = arguments.getCachedUser("user", true);
				Integer amount = arguments.getInteger("amount", true);
				if (arguments.success()) {
					new MultiGpAddCommand(multiGpCollection, user, amount, cmdUserId, sender).run();
				}
			} else if (command[1].equals("clear")) {
				User user = arguments.getCachedUser("user", true);
				if (arguments.success()) {
					new MultiGpClearCommand(multiGpCollection, user, cmdUserId, sender).run();
				}
			} else if (command[1].equals("list")) {
				new MultiGpListCommand(multiGpCollection, cmdUserId, sender).run();
			} else {
				sender.send("Error: Unrecognized Multi-GP subcommand.");
			}
		}
	}

	/**
	 * Handles an event received through a message component
	 * @param event Event
	 */
	private void handleMessageComponent(MessageComponentCreateEvent event) {
		MessageComponentInteraction interaction = event.getMessageComponentInteraction();
		ImmediateInteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
		String componentId = interaction.getCustomId();
		long cmdUserId = interaction.getUser().getId();

		switch (componentId) {
			case COMPONENT_LIST_GP_CONFIRM:
				MultiGpList gpList = multiGpCollection.get(cmdUserId);
				if (gpList == null) {
					sender.setEphemeral().setText("The multi-GP list is empty!").send();
				} else {
					try {
						EmbedBuilder gpListEmbed = gpList.toEmbed(true);
						gpList.apply(rdb);
						multiGpCollection.remove(cmdUserId);
						// Not ephemeral so the full list is posted somewhere
						sender.setText("The following Guild Points have been awarded by **" +
							interaction.getUser().getName() + "**:").addEmbed(gpListEmbed).send();
					} catch (DbOperationException e) {
						new ErrorHandler(e).sendDefaultMessage(sender).printToErrorChannel().run();
					}
				}
				break;
			case COMPONENT_LIST_GP_CLEAR:
				multiGpCollection.remove(cmdUserId);
				sender.setEphemeral().setText("Cleared multi-GP list.").send();
				break;
		}
	}

	/**
	 * Used to handle !gr commands from SpriteBot, which cannot use slash commands.
	 * @param event Message creation event
	 */
	private void handleBotGpCommand(MessageCreateEvent event) {
		if (event.getChannel().getId() == SPRITEBOT_COMMANDS_CHANNEL_ID &&
			event.getMessageAuthor().getId() == SPRITEBOT_ID) {
			String[] message = event.getMessage().getContent().split(" ");
			ChannelMsgSender privateResultSender = new ChannelMsgSender(SPRITEBOT_COMMANDS_CHANNEL_ID);

			if (message[0].equals("!gr") || message[0].equals("!tr")) {
				if (message.length == 4) {
					try {
						long userId = Long.parseLong(message[1]);
						int points = Integer.parseInt(message[2]);
						long channelId = Long.parseLong(message[3]);

						User user = api.getUserById(userId).join();
						ChannelMsgSender publicResultSender = new ChannelMsgSender(channelId);

						if (message[0].equals("!gr")) {
							new GiveGpInterfaceCommand(rdb, user, points, publicResultSender, privateResultSender,
								new NullMsgSender()).run();
						} else {
							new TakeGpInterfaceCommand(rdb, user, points, publicResultSender, privateResultSender,
								new NullMsgSender()).run();
						}
					} catch (NumberFormatException e) {
						DiscordUtils.sendJsonResult(privateResultSender, false, "Cannot parse arguments as numbers");
					}
				} else {
					DiscordUtils.sendJsonResult(privateResultSender, false, "Wrong number of arguments (3 required)");
				}
			} else if (message[0].equals("!checkr")) {
				if (message.length == 2) {
					try {
						long userId = Long.parseLong(message[1]);
						User user = api.getUserById(userId).join();

						new GetGpInterfaceCommand(rdb, user, privateResultSender, new NullMsgSender()).run();
					} catch (NumberFormatException e) {
						DiscordUtils.sendJsonResult(privateResultSender, false, "Cannot parse argument as an ID");
					}
				} else {
					DiscordUtils.sendJsonResult(privateResultSender, false, "Wrong number of arguments (1 required)");
				}
			}
		}
	}
}
