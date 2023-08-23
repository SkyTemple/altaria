package org.skytemple.altaria.features.reputation;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.*;
import org.skytemple.altaria.definitions.senders.InteractionMsgSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.utils.Utils;
import org.skytemple.altaria.definitions.db.Database;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.CommandArgumentList;

import java.util.Arrays;
import java.util.Collections;

/**
 * Class used to handle reputation commands and events
 */
public class Reputation {
	private final DiscordApi api;
	private final ReputationDB rdb;
	private final ExtConfig extConfig;

	// Used to prevent duplicated queries to the database. Gets invalidated when a GP-changing command is run.
	private Leaderboard cachedLeaderboard;

	public Reputation(Database db) {
		api = ApiGetter.get();
		rdb = new ReputationDB(db);
		extConfig = ExtConfig.get();
		cachedLeaderboard = null;

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
		.join();

		// Register commands
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
		.join();

		// Register listeners
		api.addSlashCommandCreateListener(this::handleGpCommand);
	}

	private void handleGpCommand(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();
		String[] command = interaction.getFullCommandName().split(" ");
		Logger logger = Utils.getLogger(getClass());
		logger.debug("Command received: " + interaction.getFullCommandName());
		InteractionMsgSender sender = new InteractionMsgSender(interaction);
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
				sender.send("Error: Unrecognized GP subcommand");
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
				sender.send("Error: Unrecognized GP subcommand");
			}
		}
	}
}
