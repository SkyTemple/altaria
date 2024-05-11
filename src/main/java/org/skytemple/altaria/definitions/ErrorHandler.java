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

package org.skytemple.altaria.definitions;

import org.apache.logging.log4j.Logger;
import org.skytemple.altaria.definitions.senders.ChannelMsgSender;
import org.skytemple.altaria.definitions.senders.MessageSender;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.utils.Utils;

/**
 * Class used to handle errors that take place while the bot is running. It can both log them and send them as Discord
 * messages.
 */
public class ErrorHandler {
	private static final String DEFAULT_ERROR_MESSAGE = "An error occurred while running this command.";

	private final Logger logger;
	private final String error;

	private String responseMsg;
	private Long printToChannelId;
	private MessageSender sender;

	/**
	 * Prepares a chain of calls used to log the specified error, optionally printing different information.
	 * @param error Error to log
	 */
	public ErrorHandler(Throwable error) {
		logger = Utils.getLogger(getClass());
		this.error = Utils.throwableToStr(error);

		responseMsg = null;
		printToChannelId = null;
		sender = null;
	}

	/**
	 * Sends a default error message using the specified message sender.
	 * Cannot be combined with other MessageSender methods, only the last one will be run.
	 * @param sender Sender used to send the message
	 * @return this
	 */
	public ErrorHandler sendDefaultMessage(MessageSender sender) {
		responseMsg = DEFAULT_ERROR_MESSAGE;
		this.sender = sender;
		return this;
	}

	/**
	 * Sends a custom message using the specified message sender.
	 * Cannot be combined with other MessageSender methods, only the last one will be run.
	 * @param msg Response message
	 * @param sender Sender used to send the message
	 * @return this
	 */
	public ErrorHandler sendMessage(String msg, MessageSender sender) {
		responseMsg = msg;
		this.sender = sender;
		return this;
	}

	/**
	 * Sends the full error message using the specified message sender.
	 * Cannot be combined with other MessageSender methods, only the last one will be run.
	 * @param sender Sender used to send the message
	 * @return this
	 */
	public ErrorHandler sendFullError(MessageSender sender) {
		responseMsg = getDiscordFormattedError(error);
		this.sender = sender;
		return this;
	}

	/**
	 * Prints the full error message to the default error channel, if one has been specified.
	 * @return this
	 */
	public ErrorHandler printToErrorChannel() {
		ExtConfig.get().getErrorChannelId().ifPresent(channelId -> printToChannelId = channelId);
		return this;
	}

	/**
	 * Logs the error and performs the other actions that might have been specified through previous calls.
	 */
	public void run() {
		logger.error(error);
		if (responseMsg != null) {
			sender.send(responseMsg);
		}
		if (printToChannelId != null) {
			new ChannelMsgSender(printToChannelId).send(getDiscordFormattedError(error));
		}
	}

	/**
	 * Given the full representation of an error as a string, returns another string with the Discord message that
	 * should be sent to print that error.
	 * @param error Error to convert
	 * @return Discord message representation of the error (code block + max 2000 characters)
	 */
	private String getDiscordFormattedError(String error) {
		return "```" + Utils.truncateLines(error, Constants.MAX_DISCORD_MSG_CHARS - 6) + "```";
	}
}
