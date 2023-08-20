package org.skytemple.altaria.features;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.*;
import org.skytemple.altaria.singletons.ApiGetter;
import org.skytemple.altaria.singletons.ErrorHandler;
import org.skytemple.altaria.singletons.ExtConfig;
import org.skytemple.altaria.utils.Utils;
import org.skytemple.altaria.db.Database;
import org.skytemple.altaria.db.ReputationDB;
import org.skytemple.altaria.exceptions.DbOperationException;
import org.skytemple.altaria.utils.command_arguments.CommandArgumentList;

import java.util.Arrays;
import java.util.Collections;

/**
 * Class used to handle reputation commands and events
 */
public class Reputation {
	private final DiscordApi api;
	private final ReputationDB rdb;
	private final ExtConfig extConfig;

	public Reputation(Database db) {
		api = ApiGetter.get();
		rdb = new ReputationDB(db);
		extConfig = ExtConfig.get();

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
				"leaderboard, in descending order")
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
		CommandArgumentList arguments = new CommandArgumentList(interaction, interaction);

		if (command[0].equals("gp")) {
			if (command[1].equals("add") || command[1].equals("take")) {
				User user = arguments.getCachedUser("user", true);
				Integer amount = arguments.getInteger("amount", true);
				if (arguments.success()) {
					if (amount > 0) {
						int finalAmount = command[1].equals("add") ? amount : amount * -1;
						try {
							rdb.addPoints(user.getId(), finalAmount);

							String msg;
							if (command[1].equals("add")) {
								msg = "Gave " + amount + " Guild Point(s) to ";
							} else {
								msg = "Took " + amount + " Guild Point(s) from ";
							}
							interaction.createImmediateResponder()
								.setContent(msg + "**" + user.getName() + "** (current: " +
									rdb.getPoints(user.getId()) + ")")
								.respond();
						} catch (DbOperationException e) {
							new ErrorHandler(e).defaultResponse(interaction).printToErrorChannel().run();
						}
					} else {
						interaction.createImmediateResponder()
							.setContent("Error: The amount of points must be > 0")
							.respond();
					}
				}
			} else {
				interaction.createImmediateResponder()
					.setContent("Error: Unrecognized GP subcommand")
					.respond();
			}
		} else if (command[0].equals("getgp")) {
			if (command[1].equals("check")) {
				// TODO: Should getUser be used instead? Can this cause problems?
				User user = arguments.getCachedUser("user", true);
				if (arguments.success()) {
					try {
						int amount = rdb.getPoints(user.getId());
						interaction.createImmediateResponder()
							.setContent("**" + user.getName() + "** has " + amount + " Guild Point(s)")
							.respond();
					} catch (DbOperationException e) {
						new ErrorHandler(e).defaultResponse(interaction).printToErrorChannel().run();
					}
				}
			} else if (command[1].equals("list")) {
				interaction.createImmediateResponder()
					.setContent("Not implemented yet")
					.respond();
			} else {
				interaction.createImmediateResponder()
					.setContent("Error: Unrecognized GP subcommand")
					.respond();
			}
		}
	}
}
