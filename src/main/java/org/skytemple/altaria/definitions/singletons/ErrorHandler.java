package org.skytemple.altaria.definitions.singletons;

import org.apache.logging.log4j.Logger;
import org.skytemple.altaria.definitions.Constants;
import org.skytemple.altaria.definitions.message_sender.ChannelMsgSender;
import org.skytemple.altaria.utils.FeedbackSender;
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
	private FeedbackSender feedbackSender;

	/**
	 * Prepares a chain of calls used to log the specified error, optionally printing different information.
	 * @param error Error to log
	 */
	public ErrorHandler(Throwable error) {
		logger = Utils.getLogger(getClass());
		this.error = Utils.throwableToStr(error);

		responseMsg = null;
		printToChannelId = null;
		feedbackSender = null;
	}

	/**
	 * Sends a default error message using the specified feedback sender.
	 * Cannot be combined with other FeedbackSender methods, only the last one will be run.
	 * @param sender Sender used to send the message
	 * @return this
	 */
	public ErrorHandler sendDefaultMessage(FeedbackSender sender) {
		responseMsg = DEFAULT_ERROR_MESSAGE;
		feedbackSender = sender;
		return this;
	}

	/**
	 * Sends a custom message using the specified feedback sender.
	 * Cannot be combined with other FeedbackSender methods, only the last one will be run.
	 * @param msg Response message
	 * @param sender Sender used to send the message
	 * @return this
	 */
	public ErrorHandler sendMessage(String msg, FeedbackSender sender) {
		responseMsg = msg;
		feedbackSender = sender;
		return this;
	}

	/**
	 * Sends the full error message using the specified feedback sender.
	 * Cannot be combined with other FeedbackSender methods, only the last one will be run.
	 * @param sender Sender used to send the message
	 * @return this
	 */
	public ErrorHandler sendFullError(FeedbackSender sender) {
		responseMsg = getDiscordFormattedError(error);
		feedbackSender = sender;
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
			feedbackSender.error(responseMsg);
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
