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

package org.skytemple.altaria.features.auto_timeout;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.db.AutoTimeoutDB;
import org.skytemple.altaria.definitions.db.Database;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.ChannelMsgSender;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.utils.DurationParser;
import org.skytemple.altaria.utils.Utils;

import java.time.Duration;

/**
 * Used to automatically timeout members when they receive a Vortex strike, since Vortex cannot do it on its own.
 */
public class AutoTimeout {
	private static final long VORTEX_ID = 240254129333731328L;

	private final DiscordApi api;
	private final ExtConfig extConfig;
	private final AutoTimeoutDB db;
	private final Logger logger;

	public AutoTimeout(Database db) {
		api = ApiGetter.get();
		extConfig = ExtConfig.get();
		this.db = new AutoTimeoutDB(db);
		logger = Utils.getLogger(getClass());

		if (extConfig.strikeTimeoutsEnabled()) {
			// Register listeners
			api.addMessageCreateListener(this::handleMsgEvent);
		}
	}

	private void handleMsgEvent(MessageCreateEvent event) {
		String message = event.getMessage().getContent();
		ChannelMsgSender sender = new ChannelMsgSender(event.getChannel().getId());

		if (message.startsWith(">>punishment")) {
			// Errors are ignored, since Vortex won't process the command either.

			String[] splitMsg = message.split(" ");
			if (splitMsg.length == 3 || splitMsg.length == 4) {
				int numStrikes;
				try {
					numStrikes = Integer.parseInt(splitMsg[1]);
				} catch (NumberFormatException e) {
					return;
				}
				if (numStrikes >= 1) {
					String action = splitMsg[2];
					try {
						if (splitMsg.length == 3 && action.equals("none")) {
							db.setDuration(numStrikes, 0);
							logger.info("Deleted auto-mute punishment for " + numStrikes + " strikes.");
						} else if (splitMsg.length == 4 && (action.equals("mute") || action.equals("tempmute")
						|| action.equals("temp-mute"))) {
							String durationStr = splitMsg[3];
							Duration timeSeconds;
							try {
								// DurationParser can't parse more complex time strings (which Vortex does support),
								// but this should be enough.
								timeSeconds = DurationParser.parse(durationStr);
							} catch (IllegalArgumentException e) {
								sender.send("Error: Invalid time string. If Vortex accepted the command, try again " +
									"with a simpler time format string (number + single letter) to ensure I accept " +
									"it too.");
								return;
							}
							db.setDuration(numStrikes, (int) timeSeconds.toSeconds());
							logger.info("Updated auto-mute punishment for " + numStrikes + " strikes: " + timeSeconds +
								" seconds.");
						}
					} catch (DbOperationException e) {
						new ErrorHandler(e).sendMessage("Error updating mute duration", sender)
							.printToErrorChannel().run();
					}
				}
			}
		} else {
			if (event.getMessageAuthor().getId() == VORTEX_ID &&
			event.getChannel().getId() == extConfig.getStrikeLogChannelId()) {
				VortexStrikeParser.Strike strike = VortexStrikeParser.parse(message);
				if (strike != null) {
					int muteDuration;
					try {
						muteDuration = db.getDuration(strike.newNumStrikes());
					} catch (DbOperationException e) {
						new ErrorHandler(e).printToErrorChannel().run();
						return;
					}
					@SuppressWarnings("UnnecessaryUnicodeEscape") // Doesn't get displayed properly otherwise
					String reasonMsg = "[" + strike.oldNumStrikes() + " \u2192 " + strike.newNumStrikes() + " strikes]: " +
						strike.reason();
					Server server = event.getServer().orElse(null);
					if (server != null) {
						strike.user().timeout(server, Duration.ofSeconds(muteDuration), reasonMsg);
						logger.debug("Automatically timed-out " + strike.user().getName() + " for "+
							muteDuration + " seconds.");
					} else {
						logger.error("Cannot get server associated to strike message. Message ID: " +
							event.getMessageId());
					}
				}
			}
		}
	}
}
