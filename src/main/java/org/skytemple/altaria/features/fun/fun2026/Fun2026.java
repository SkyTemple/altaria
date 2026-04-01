/*
 * Copyright (c) 2025-2026. Frostbyte and other contributors.
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

package org.skytemple.altaria.features.fun.fun2026;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Attachment;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.*;
import org.skytemple.altaria.definitions.*;
import org.skytemple.altaria.definitions.db.Database;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.role_matcher.RoleMatch;
import org.skytemple.altaria.definitions.role_matcher.RoleMatcher;
import org.skytemple.altaria.definitions.senders.DelayedInteractionMsgSender;
import org.skytemple.altaria.definitions.senders.ImmediateInteractionMsgSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.utils.DurationFormatter;
import org.skytemple.altaria.utils.JavacordUtils;

import java.awt.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements the April Fools 2026 commands
 */
public class Fun2026 {
	private static final Pattern HEX_COLOR_REGEX = Pattern.compile("#?([0-9A-Fa-f]{6})");
	private static final int DEFAULT_COST_HALF_LIFE_MINUTES = 30;
	private static final int MAX_PENDING_RECOLOR_ACTIONS = 50;
	private static final int COMMAND_COOLDOWN_SECONDS = 5 * 60;
	private static final String RECOLOR_ROLE_BUTTON_ID = "fun2026RecolorRole";

	private final DiscordApi api;
	private final ReputationDB rdb;
	private final User selfUser;

	// Current costs to recolor each role
	private final RoleRecolorCosts recolorCosts;
	// Pending actions (confirmed by pressing an UI button)
	private final ButtonActionList<RecolorButtonAction> actionList;
	private final CommandCooldown commandCooldown;

	public Fun2026(Database db, CommandCreator commandCreator) {
		api = ApiGetter.get();
		rdb = new ReputationDB(db);
		selfUser = api.getYourself();

		recolorCosts = new RoleRecolorCosts(DEFAULT_COST_HALF_LIFE_MINUTES);
		actionList = new ButtonActionList<>(RECOLOR_ROLE_BUTTON_ID, MAX_PENDING_RECOLOR_ACTIONS);
		commandCooldown = new CommandCooldown();

		// Register commands
		commandCreator.registerCommand(
			SlashCommand.with("rolecolor", "Recolor a role", Arrays.asList(
				SlashCommandOption.create(SlashCommandOptionType.STRING, "role", "Name of the role to recolor " +
					"(partial matches supported)", true),
				SlashCommandOption.create(SlashCommandOptionType.STRING, "color_or_role", "Hex code of the color to " +
					"use, or role to copy the color from. Omit to get the current recolor cost.", false)
				))
			.setDefaultDisabled()
		);
		commandCreator.registerCommand(
			SlashCommand.with("rolecolorbackup", "Backup or restore role colors", Arrays.asList(
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "dump", "Dump current colors",
					Collections.singletonList(
						SlashCommandOption.create(SlashCommandOptionType.STRING, "format", "Format to use for the " +
							"dump. Valid values are 'csv' and 'text'.", true)
					)
				),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "load", "Load colors from CSV",
					Collections.singletonList(
						SlashCommandOption.create(SlashCommandOptionType.ATTACHMENT, "file", "CSV to load role " +
							"colors from.", true)
					)
				)
			))
			.setDefaultDisabled()
		);
		commandCreator.registerCommand(
			SlashCommand.with("rolecolorhalflife", "Set half-life for the exponential decay of the /rolecolor command cost",
				Collections.singletonList(
					SlashCommandOption.create(SlashCommandOptionType.LONG, "half-life", "Half-life in minutes", true)
				))
			.setDefaultDisabled()
		);

		// Register listeners
		api.addSlashCommandCreateListener(this::handleFunCommand);
		api.addSlashCommandCreateListener(this::handleDelayedFunCommand);
		api.addMessageComponentCreateListener(this::handleMessageComponent);
	}

	private void handleFunCommand(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();
		String[] command = interaction.getFullCommandName().split(" ");
		ImmediateInteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
		CommandArgumentList arguments = new CommandArgumentList(interaction, sender);

		if (command[0].equals("rolecolor")) {
			Server server = interaction.getServer().orElse(null);
			if (server == null) {
				sender.send("Error: This command can only be used in a server.");
				return;
			}

			User user = interaction.getUser();
			String roleName = arguments.getString("role", true);
			String colorOrRole = arguments.getString("color_or_role", false);

			if (arguments.success()) {
				RoleMatcher roleMatcher = new RoleMatcher(server);
				RoleMatch firstRoleMatch = roleMatcher.findRole(roleName);

				if (firstRoleMatch.getNumMatches() != 1) {
					sender.setEphemeral().send(firstRoleMatch.getDefaultMessage());
					return;
				}

				Role roleToUpdate = firstRoleMatch.getFirstMatch();
				int cost = recolorCosts.getCost(roleToUpdate.getId());

				if (colorOrRole == null) {
					// Return current cost only
					sender.send("Current cost to recolor the **" + roleToUpdate.getName() + "** role: " +
						cost + " GP.");
				} else {
					// Actually run the command
					Role roleToCopyFrom;
					Color color;

					Matcher regexMatcher = HEX_COLOR_REGEX.matcher(colorOrRole);
					if (regexMatcher.matches()) {
						// Set color to the one specified as a hex string
						color = Color.decode("#" + regexMatcher.group(1));
						roleToCopyFrom = null;
					} else {
						// Copy color from role
						RoleMatch secondRoleMatch = roleMatcher.findRole(colorOrRole);

						if (secondRoleMatch.getNumMatches() != 1) {
							sender.setEphemeral().send(secondRoleMatch.getDefaultMessage());
							return;
						}

						roleToCopyFrom = secondRoleMatch.getFirstMatch();
						// If the role doesn't have a color, use #000000 to clear it.
						color = roleToCopyFrom.getColor().orElse(new Color(0, 0 ,0));
					}

					// Check cooldown
					long cooldown = commandCooldown.remainingCooldown(user.getId());
					if (
						cooldown > 0 && !JavacordUtils.hasGlobalPermission(user, server, PermissionType.ADMINISTRATOR)
					) {
						String cooldownFormatted = new DurationFormatter(Duration.ofSeconds(cooldown)).toUserFormat();
						sender.setEphemeral().send("You can use this command again in " + cooldownFormatted + ".");
						return;
					}

					new RoleColorCommand(rdb, user, selfUser, roleToUpdate, color, roleToCopyFrom, cost, actionList,
						sender, sender).run();
				}
			}
		} else if (command[0].equals("rolecolorhalflife")) {
			int halfLife = arguments.getInteger("half-life", true);

			if (arguments.success()) {
				recolorCosts.setHalfLife(halfLife);
				sender.setEphemeral().send("Cost half-life set to " + halfLife + " minutes.");
			}
		}
	}

	private void handleDelayedFunCommand(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();
		String[] command = interaction.getFullCommandName().split(" ");

		if (command[0].equals("rolecolorbackup")) {
			DelayedInteractionMsgSender sender = new DelayedInteractionMsgSender(interaction, false);
			CommandArgumentList arguments = new CommandArgumentList(interaction, sender);
			Server server = interaction.getServer().orElse(null);

			if (server == null) {
				sender.send("Error: This command can only be used in a server.");
				return;
			}

			if (command[1].equals("dump")) {
				String format = arguments.getString("format", true);

				if (arguments.success()) {
					boolean textDump;
					if (format.equals("text")) {
						textDump = true;
					} else if (format.equals("csv")) {
						textDump = false;
					} else {
						sender.send("Invalid format '" + format + "'. Valid values are 'text' and 'csv'.");
						return;
					}

					new RoleColorDumpCommand(server, textDump, sender).run();
				}
			} else if (command[1].equals("load")) {
				Attachment attachment = arguments.getAttachment("file", true);

				if (arguments.success()) {
					new RoleColorLoadCommand(server, attachment, sender, sender).run();
				}
			}
		}
	}

	private void handleMessageComponent(MessageComponentCreateEvent event) {
		MessageComponentInteraction interaction = event.getMessageComponentInteraction();
		ImmediateInteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
		String customInteractionId = interaction.getCustomId();
		long cmdUserId = interaction.getUser().getId();

		if (actionList.shouldHandleInteraction(customInteractionId)) {
			RecolorButtonAction recolorAction = actionList.popAction(customInteractionId);

			if (recolorAction == null) {
				sender.setEphemeral().send("The button you clicked has expired. Please run the command again.");
				return;
			}

			Role roleToUpdate = recolorAction.roleToUpdate;
			int cost = recolorCosts.getCost(roleToUpdate.getId());

			int userGP;
			try {
				userGP = rdb.getPointsInt(cmdUserId);
			} catch (DbOperationException e) {
				new ErrorHandler(e).sendDefaultMessage(sender).printToErrorChannel().run();
				return;
			}

			// Cost might have changed since the command was run, check again
			if (userGP < cost) {
				sender.setEphemeral()
					.send("Recoloring the **" + roleToUpdate.getName() + "** role currently requires " + cost +
						" GP, but you only have " + userGP + ".\n" +
						"Cost will decrease over time if no one recolors the role, so try again later!");
				return;
			}

			// Recolor role
			try {
				roleToUpdate.updateColor(recolorAction.color).join();
			} catch (CancellationException | CompletionException e) {
				new ErrorHandler(e).sendDefaultMessage(sender).printToErrorChannel().run();
				return;
			}

			// Update cost
			recolorCosts.setCost(roleToUpdate.getId(), cost + 1);
			// Update cooldown
			commandCooldown.setCooldown(cmdUserId, COMMAND_COOLDOWN_SECONDS);

			// Update user GP
			try {
				rdb.addPoints(cmdUserId, cost * -1);
			} catch (DbOperationException e) {
				new ErrorHandler(e).sendDefaultMessage(sender).printToErrorChannel().run();
				// The role still got recolored, but whatever
				return;
			}

			EmbedBuilder embed = new EmbedBuilder();
			if (recolorAction.color.equals(new Color(0, 0, 0))) {
				embed.setDescription("<@" + cmdUserId + "> cleared the color of the <@&" + roleToUpdate.getId() + "> " +
					"role!");
			} else if (recolorAction.roleCopiedFrom == null) {
				String colorHex = String.format("#%06X", recolorAction.color.getRGB() & 0xFFFFFF);
				embed.setDescription("<@" + cmdUserId + "> changed the color of the <@&" + roleToUpdate.getId() + "> " +
					"role to " + colorHex + "!");
				embed.setColor(recolorAction.color);
			} else {
				embed.setDescription("<@" + cmdUserId + "> changed the color of the <@&" + roleToUpdate.getId() + "> " +
					"role to match the <@&" + recolorAction.roleCopiedFrom.getId() + "> role!");
				embed.setColor(recolorAction.color);
			}

			if (cost > 0) {
				embed.setFooter("Cost: " + cost + " GP");
			}
			sender.addEmbed(embed).send();
		}
	}

	public record RecolorButtonAction(Role roleToUpdate, Color color, Role roleCopiedFrom) {}
}
