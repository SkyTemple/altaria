/*
 * Copyright (c) 2025. Frostbyte and other contributors.
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

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.skytemple.altaria.definitions.CommandArgumentList;
import org.skytemple.altaria.definitions.CommandCooldown;
import org.skytemple.altaria.definitions.CommandCreator;
import org.skytemple.altaria.definitions.senders.ImmediateInteractionMsgSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.utils.DurationFormatter;
import org.skytemple.altaria.utils.JavacordUtils;

import java.time.Duration;
import java.util.Collections;

/**
 * Implements the April Fools 2025 commands
 */
public class Fun {
	private final DiscordApi api;
	private final ExtConfig extConfig;
	private final Long channelId;
	private final Long roleId;
	private final long cooldownTime;

	private final CommandCooldown infectCommandCooldown;

	public Fun(CommandCreator commandCreator) {
		api = ApiGetter.get();
		extConfig = ExtConfig.get();
		channelId = extConfig.getFun2025ChannelId();
		roleId = extConfig.getFun2025RoleId();
		cooldownTime = extConfig.getFun2025Cooldown();

		infectCommandCooldown = new CommandCooldown();

		if (channelId != null && roleId != null) {
			// Register commands
			commandCreator.registerCommand(
				SlashCommand.with("infect", "Infect another user", Collections.singletonList(
					SlashCommandOption.create(SlashCommandOptionType.USER, "user", "User to infect", true)
				))
				.setDefaultDisabled()
			);

			// Register listeners
			api.addSlashCommandCreateListener(this::handleFunCommand);
		}
	}

	private void handleFunCommand(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();
		String[] command = interaction.getFullCommandName().split(" ");
		ImmediateInteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
		CommandArgumentList arguments = new CommandArgumentList(interaction, sender);

		if (command[0].equals("infect")) {
			User user = interaction.getUser();
			User targetUser = arguments.getCachedUser("user", true);
			if (arguments.success()) {
				long cooldown = infectCommandCooldown.remainingCooldown(user.getId());

				if (
					cooldown > 0 &&
					!JavacordUtils.hasGlobalPermission(user, extConfig.getServer(), PermissionType.ADMINISTRATOR)
				) {
					String cooldownFormatted = new DurationFormatter(Duration.ofSeconds(cooldown)).toUserFormat();
					sender.setEphemeral().send("You can use this command again in " + cooldownFormatted + ".");
					return;
				}

				InfectCommand infectCommand = new InfectCommand(
					extConfig.getServer(), user, targetUser, sender, sender);
				infectCommand.run();

				if (infectCommand.success) {
					infectCommandCooldown.setCooldown(user.getId(), cooldownTime);
				}
			}
		}
	}
}
