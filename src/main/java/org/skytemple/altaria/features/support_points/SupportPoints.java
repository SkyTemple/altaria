/*
 * Copyright (c) 2023-2024. End45 and other contributors.
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

package org.skytemple.altaria.features.support_points;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.event.interaction.MessageContextMenuCommandEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.interaction.*;
import org.skytemple.altaria.definitions.*;
import org.skytemple.altaria.definitions.db.Database;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.db.SupportThreadsDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.DelayedInteractionMsgSender;
import org.skytemple.altaria.definitions.senders.ImmediateInteractionMsgSender;
import org.skytemple.altaria.definitions.senders.InteractionMsgSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.utils.JavacordUtils;
import org.skytemple.altaria.utils.Utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.skytemple.altaria.features.support_points.SupportGpSwitcher.COMPONENT_SUPPORT_GP_DISABLE;
import static org.skytemple.altaria.features.support_points.SupportGpSwitcher.COMPONENT_SUPPORT_GP_ENABLE;

/**
 * Class used to give GP based on activity on a support channel
 */
public class SupportPoints {
	// Component IDs
	public static final String COMPONENT_SUPPORT_GP_CONFIRM = "supportGpConfirm";

	// Context action IDs
	private static final String SWITCH_GP_CONTEXT_ACTION = "Support GP switch";

	private final DiscordApi api;
	private final ReputationDB rdb;
	private final SupportThreadsDB sdb;
	private final ExtConfig extConfig;
	private final Logger logger;

	// Used to store the amount of points to give to each user. One entry for each user who run the "calc" command.
	private final MultiGpCollection multiGpCollection;
	// Used to store the dates specified when running the "calc" command. One entry for each user who run the command.
	private final Map<Long, DateRange> userDates;
	// Class used to handle thread support GP switch actions
	private final SupportGpSwitcher supportGpSwitcher;

	// ID of the channel used to calculate the points
	private final long supportChannelId;

	public SupportPoints(Database db, CommandCreator commandCreator) {
		api = ApiGetter.get();
		rdb = new ReputationDB(db);
		sdb = new SupportThreadsDB(db);
		extConfig = ExtConfig.get();
		logger = Utils.getLogger(getClass());
		multiGpCollection = new MultiGpCollection();
		userDates = new TreeMap<>();
		supportGpSwitcher = new SupportGpSwitcher(sdb);
		supportChannelId = extConfig.getSupportChannelId();

		Channel _supportChannel = api.getChannelById(supportChannelId).orElse(null);
		if (_supportChannel instanceof ServerTextChannel || _supportChannel instanceof ServerForumChannel) {
			// Register commands
			commandCreator.registerCommand(
				SlashCommand.with("supportgp", "Commands to handle GP awarded for support contributions", Arrays.asList(
					SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "check", "Check how many " +
							"points would be awarded for contributions to a given thread",
						Collections.singletonList(
							SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "thread", "Thread to check", true)
						)
					),
					SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "calc", "Calculate points for " +
							"a date range",
						Arrays.asList(
							SlashCommandOption.create(SlashCommandOptionType.STRING, "startDate", "Start date " +
								"(ISO-8601 Date + Time + Offset format, e.g. \"2011-12-03T10:15:30+01:00\")", true),
							SlashCommandOption.create(SlashCommandOptionType.STRING, "endDate", "End date " +
								"(ISO-8601 Date + Time + Offset format). Omit to use current date.", false)
						)
					),
					SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "switch", "Enable/Disable " +
							"support GP for a user in the current thread",
						Arrays.asList(
							SlashCommandOption.create(SlashCommandOptionType.USER, "user", "User to enable/disable GP " +
								"for", true),
							SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "thread", "Thread where GP " +
								"should be enabled/disabled for the user. Default: Current thread.", false)
						)
					)
				))
				.setDefaultDisabled()
			);

			// Register context actions
			commandCreator.registerCommand(
				MessageContextMenu.with(SWITCH_GP_CONTEXT_ACTION)
				.setDefaultDisabled()
			);

			// Register listeners
			api.addSlashCommandCreateListener(this::handleSupportGpCommand);
			api.addMessageComponentCreateListener(this::handleMessageComponent);
			api.addMessageCreateListener(this::handleThreadMessage);
			api.addMessageDeleteListener(this::handleThreadMessageDeletion);
			api.addMessageContextMenuCommandListener(this::handleContextAction);
		} else {
			logger.error("Support channel with ID " + extConfig.getSupportChannelId() + " does not exist or is not a " +
				"server text channel. SupportGP commands will be disabled.");
		}
	}

	private void handleSupportGpCommand(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();
		String[] command = interaction.getFullCommandName().split(" ");
		long cmdUserId = interaction.getUser().getId();

		if (command[0].equals("supportgp")) {
			if (command[1].equals("check")) {
				DelayedInteractionMsgSender sender = new DelayedInteractionMsgSender(interaction, true);
				CommandArgumentList arguments = new CommandArgumentList(interaction, sender);

				Channel channel = arguments.getChannel("thread", true);
				if (arguments.success()) {
					ServerThreadChannel thread = channel.asServerThreadChannel().orElse(null);
					if (thread != null) {
						logger.debug("Number of messages: " + thread.getMessageCount());
						new SupportGpCheckCommand(sdb, thread, sender, sender).run();
					} else {
						sender.send("Error: Specified channel is not a thread");
					}
				}
			} else if (command[1].equals("calc")) {
				DelayedInteractionMsgSender sender = new DelayedInteractionMsgSender(interaction, true);
				CommandArgumentList arguments = new CommandArgumentList(interaction, sender);

				String startDateStr = arguments.getString("startDate", true);
				String endDateStr = arguments.getString("endDate", false);
				if (arguments.success()) {
					long endTimestamp;
					long startTimestamp =
						ZonedDateTime.parse(startDateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toEpochSecond();
					if (endDateStr != null) {
						endTimestamp =
							ZonedDateTime.parse(endDateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toEpochSecond();
					} else {
						endTimestamp = System.currentTimeMillis() / 1000;
					}
					new SupportGpCalcCommand(sdb, supportChannelId, startTimestamp, endTimestamp, sender, sender, gpList -> {
						multiGpCollection.put(cmdUserId, gpList);
						userDates.put(cmdUserId, new DateRange(startTimestamp, endTimestamp));
					}).run();
				}
			} else if (command[1].equals("switch")) {
				InteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
				CommandArgumentList arguments = new CommandArgumentList(interaction, sender);

				User user = arguments.getCachedUser("user", true);
				Channel channel = arguments.getChannel("thread", false);
				if (arguments.success()) {
					if (channel == null) {
						channel = interaction.getChannel().orElse(null);
					}
					if (channel != null) {
						ServerThreadChannel thread = channel.asServerThreadChannel().orElse(null);
						if (thread != null) {
							// Not worth creating a command class to pass 7 args and call a function
							supportGpSwitcher.showSupportGpSwitchMenu(thread, user.getId(), user.getName(), cmdUserId,
								sender, sender);
						} else {
							sender.setEphemeral().send("Error: Specified channel is not a thread");
						}
					} else {
						sender.setEphemeral().send("Error: No channel specified and current channel could not " +
							"be retrieved");
					}
				}
			} else {
				InteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
				sender.send("Error: Unrecognized support GP subcommand.");
			}
		}
	}

	private void handleMessageComponent(MessageComponentCreateEvent event) {
		MessageComponentInteraction interaction = event.getMessageComponentInteraction();
		ImmediateInteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
		String componentId = interaction.getCustomId();
		long cmdUserId = interaction.getUser().getId();

		switch (componentId) {
			case COMPONENT_SUPPORT_GP_CONFIRM:
				MultiGpList gpList = multiGpCollection.get(cmdUserId);
				DateRange dateRange = userDates.get(cmdUserId);
				if (gpList == null) {
					sender.setEphemeral().setText("Error: No action to confirm. Run /supportgp calc first.").send();
				} else {
					try {
						EmbedBuilder gpListEmbed = gpList.toEmbed(true);
						gpList.apply(rdb);
						multiGpCollection.remove(cmdUserId);
						userDates.remove(cmdUserId);
						// Not ephemeral so the full list is posted somewhere
						sender.setText("The following Guild Points have been awarded by **" +
								interaction.getUser().getName() + "** for support contributions from <t:" +
								dateRange.startTimestamp + "> to <t:" + dateRange.endTimestamp + ">:")
							.addEmbed(gpListEmbed).send();
					} catch (DbOperationException e) {
						new ErrorHandler(e).sendDefaultMessage(sender).printToErrorChannel().run();
					}
				}
				break;
			case COMPONENT_SUPPORT_GP_ENABLE:
				if (!supportGpSwitcher.confirmSupportGpSwitch(cmdUserId, true, sender, sender)) {
					interaction.acknowledge();
				}
				break;
			case COMPONENT_SUPPORT_GP_DISABLE:
				if (!supportGpSwitcher.confirmSupportGpSwitch(cmdUserId, false, sender, sender)) {
					interaction.acknowledge();
				}
				break;
		}
	}

	/**
	 * Triggered when a new message is posted. If the message was posted on a support thread that's currently cached,
	 * updates its total message count.
	 * This is necessary because Javacord caches thread data, and it never updates the message count. It's also not
	 * possible to manually request an up-to-date version of the channel.
	 * @param event Message creation event
	 */
	private void handleThreadMessage(MessageCreateEvent event) {
		ServerThreadChannel thread = event.getServerThreadChannel().orElse(null);
		if (thread != null && thread.getParent().getId() == extConfig.getSupportChannelId() &&
			!event.getMessage().getFlags().contains(MessageFlag.EPHEMERAL)) {
			JavacordUtils.updateThreadMessageCount(thread, 1);
		}
	}

	/**
	 * Triggered when a message is deleted. If the message was deleted on a support thread that's currently cached,
	 * updates its total message count.
	 * @param event Message deletion event
	 */
	private void handleThreadMessageDeletion(MessageDeleteEvent event) {
		ServerThreadChannel thread = event.getServerThreadChannel().orElse(null);
		if (thread != null && thread.getParent().getId() == extConfig.getSupportChannelId()) {
			JavacordUtils.updateThreadMessageCount(thread, -1);
		}
	}

	private void handleContextAction(MessageContextMenuCommandEvent event) {
		MessageContextMenuInteraction interaction = event.getMessageContextMenuInteraction();
		ImmediateInteractionMsgSender sender = new ImmediateInteractionMsgSender(interaction);
		String cmdName = interaction.getCommandName();
		if (cmdName.equals(SWITCH_GP_CONTEXT_ACTION)) {
			Optional<TextChannel> optChannel = interaction.getChannel();
			if (optChannel.isPresent()) {
				ServerThreadChannel thread = optChannel.get().asServerThreadChannel().orElse(null);
				if (thread != null && thread.getParent().getId() == extConfig.getSupportChannelId()) {
					MessageAuthor messageAuthor = interaction.getTarget().getAuthor();
					long cmdUserId = interaction.getUser().getId();
					long authorId = messageAuthor.getId();
					String authorName = messageAuthor.getName();
					supportGpSwitcher.showSupportGpSwitchMenu(thread, authorId, authorName, cmdUserId, sender, sender);
				} else {
					sender.setEphemeral().send("Error: This action can only be used in support threads.");
				}
			}
		}
	}

	/**
	 * Used to store a pair of timestamps that represent a time range in Unix seconds.
	 * @param startTimestamp Start timestamp
	 * @param endTimestamp End timestamp
	 */
	private record DateRange(long startTimestamp, long endTimestamp) {}
}
