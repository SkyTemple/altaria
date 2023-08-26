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

package org.skytemple.altaria.features.rules;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedField;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.skytemple.altaria.definitions.CommandArgumentList;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.senders.InteractionMsgSender;
import org.skytemple.altaria.definitions.senders.MessageSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.utils.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to check the server rules with a command
 */
public class Rules {
	// Seconds to cache the rules for
	private static final int RULES_CACHE_TIME = 60*30;
	private static final Pattern RULE_REGEX = Pattern.compile("\\*\\*Rule (.*)\\*\\*\\n");

	private final DiscordApi api;
	private final ExtConfig extConfig;
	private final Logger logger;
	private final Long rulesMessageId;
	// Maps rule IDs to their content
	private Map<String, String> rules;
	// Last time the rules map was updated
	private long lastRulesUpdate;

	public Rules() {
		api = ApiGetter.get();
		extConfig = ExtConfig.get();
		rulesMessageId = extConfig.rulesMessageId();
		logger = Utils.getLogger(getClass());

		if (rulesMessageId != null) {
			// Register commands
			SlashCommand.with("rule", "Display one of the server rules", Collections.singletonList(
				// Technically not a number since rules can also have letters in their ID
				SlashCommandOption.create(SlashCommandOptionType.STRING, "number", "Rule number", true)
			))
			.createForServer(api, extConfig.getGuildId())
			.join();
		}
	}

	private void handleRulesCommand(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();
		String[] command = interaction.getFullCommandName().split(" ");
		InteractionMsgSender sender = new InteractionMsgSender(interaction);
		CommandArgumentList arguments = new CommandArgumentList(interaction, sender);

		if (command[0].equals("rule")) {
			String ruleNumber = arguments.getString("number", true);
			if (arguments.success()) {
				updateRules(sender);
				new RulesCommand(rules, ruleNumber, sender, sender).run();
			}
		}
	}

	/**
	 * Reads the rules message on the server, parses it and updates the rules map. If the map was updated less than
	 * {@link #RULES_CACHE_TIME} seconds ago, nothing will happen.
	 * @param errorSender Used to send error messages.
	 */
	private void updateRules(MessageSender errorSender) {
		if (rules == null || lastRulesUpdate + RULES_CACHE_TIME < System.currentTimeMillis()) {
			Channel rulesChannel = api.getChannelById(extConfig.rulesChannelId()).orElse(null);
			if (rulesChannel != null) {
				TextChannel rulesTextChannel = rulesChannel.asTextChannel().orElse(null);
				if (rulesTextChannel != null) {
					Message rulesMessage = api.getMessageById(extConfig.rulesMessageId(), rulesTextChannel)
						.exceptionally(e -> {
							new ErrorHandler(e).sendMessage("Error: Cannot retrieve rules message.", errorSender).run();
							return null;
					}).join();
					if (rulesMessage != null) {
						List<Embed> rulesEmbedList = rulesMessage.getEmbeds();
						if (!rulesEmbedList.isEmpty()) {
							rules = new HashMap<>();
							Embed rulesEmbed = rulesEmbedList.get(0);
							for (EmbedField field : rulesEmbed.getFields()) {
								String content = field.getValue();
								Matcher matcher = RULE_REGEX.matcher(content);
								if (matcher.matches()) {
									String ruleNumber = matcher.group(1);
									if (ruleNumber != null) {
										rules.put(ruleNumber, content);
									} else {
										errorSender.send("Error: Cannot find rule ID for one of the rules.");
										logger.error("Cannot find rule ID for the following rule:\n" + content);
										break;
									}
								}
							}
							lastRulesUpdate = System.currentTimeMillis();
							logger.debug("Matched " + rules.size() + " rules");
						} else {
							errorSender.send("Error: The rules message doesn't have an embed.");
						}
					}
				} else {
					errorSender.send("Error: The specified rules channel is not a text channel.");
				}
			} else {
				errorSender.send("Error: The specified rules channel does not exist.");
			}
		}
	}
}
