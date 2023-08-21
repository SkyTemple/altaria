package org.skytemple.altaria.features;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.*;
import org.skytemple.altaria.definitions.senders.InteractionMsgSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.definitions.senders.FeedbackSender;
import org.skytemple.altaria.utils.Utils;
import org.skytemple.altaria.definitions.db.Database;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
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
		InteractionMsgSender sender = new InteractionMsgSender(interaction);
		CommandArgumentList arguments = new CommandArgumentList(interaction, sender);

		if (command[0].equals("gp")) {
			if (command[1].equals("add") || command[1].equals("take")) {
				User user = arguments.getCachedUser("user", true);
				Integer amount = arguments.getInteger("amount", true);
				if (arguments.success()) {
					if (command[1].equals("add")) {
						runGiveGp(user, amount, FeedbackSender.fullFeedback(sender, sender));
					} else {
						runTakeGp(user, amount, FeedbackSender.fullFeedback(sender, sender));
					}
				}
			} else {
				sender.send("Error: Unrecognized GP subcommand");
			}
		} else if (command[0].equals("getgp")) {
			if (command[1].equals("check")) {
				// TODO: Should getUser be used instead? Can this cause problems?
				User user = arguments.getCachedUser("user", true);
				if (arguments.success()) {
					runGetGp(user, FeedbackSender.fullFeedback(sender, sender));
				}
			} else if (command[1].equals("list")) {
				sender.send("Not implemented yet");
			} else {
				sender.send("Error: Unrecognized GP subcommand");
			}
		}
	}

	/**
	 * Gives GP to the specified user
	 * @param user User to give the points to
	 * @param amount Amount of points to give. Must be greater than 0.
	 * @param feedbackSender Object used to send feedback messages
	 */
	public void runGiveGp(User user, int amount, FeedbackSender feedbackSender) {
		if (amount > 0) {
			runChangeGp(user, amount, feedbackSender);
		} else {
			feedbackSender.error("Error: The amount of points must be > 0");
		}
	}

	/**
	 * Takes GP from the specified user
	 * @param user User to give the points to
	 * @param amount Amount of points to give. Must be greater than 0.
	 * @param feedbackSender Object used to send feedback messages
	 */
	public void runTakeGp(User user, int amount, FeedbackSender feedbackSender) {
		if (amount > 0) {
			runChangeGp(user, amount * -1, feedbackSender);
		} else {
			feedbackSender.error("Error: The amount of points must be > 0");
		}
	}

	/**
	 * Gets the amount of GP a user has
	 * @param user User whose GP will be checked
	 * @param feedback Object used to send feedback messages
	 */
	public void runGetGp(User user, FeedbackSender feedback) {
		try {
			int amount = rdb.getPoints(user.getId());
			feedback.result("**" + user.getName() + "** has " + amount + " Guild Point(s)");
		} catch (DbOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(feedback).printToErrorChannel().run();
		}
	}

	/**
	 * Modifies the GP count of an user
	 * @param user User whose GP will be modified
	 * @param amount Amount to give (if > 0) or take (if < 0)
	 * @param feedback Used to send feedback to the user
	 */
	private void runChangeGp(User user, int amount, FeedbackSender feedback) {
		try {
			rdb.addPoints(user.getId(), amount);

			String msg;
			if (amount >= 0) {
				msg = "Gave " + amount + " Guild Point(s) to ";
			} else {
				msg = "Took " + amount * -1 + " Guild Point(s) from ";
			}
			feedback.result(msg + "**" + user.getName() + "** (current: " + rdb.getPoints(user.getId()) + ")");
		} catch (DbOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(feedback).printToErrorChannel().run();
		}
	}
}
