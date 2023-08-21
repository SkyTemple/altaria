package org.skytemple.altaria.definitions.singletons;

import org.apache.logging.log4j.Logger;
import org.javacord.api.interaction.InteractionBase;
import org.skytemple.altaria.definitions.Constants;
import org.skytemple.altaria.utils.DiscordUtils;
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
	private InteractionBase interaction;

	/**
	 * Prepares a chain of calls used to log the specified error, optionally printing different information.
	 * @param error Error to log
	 */
	public ErrorHandler(Throwable error) {
		logger = Utils.getLogger(getClass());
		this.error = Utils.throwableToStr(error);

		responseMsg = null;
		printToChannelId = null;
		interaction = null;
	}

	/**
	 * Responds to the interaction that caused the error with the specified message.
	 * Cannot be combined with other interaction response methods, only the last one will be run.
	 * @param msg Response message
	 * @param interaction Interaction to use to respond
	 * @return this
	 */
	public ErrorHandler response(String msg, InteractionBase interaction) {
		responseMsg = msg;
		this.interaction = interaction;
		return this;
	}

	/**
	 * Responds to the interaction that caused the error with a default error message.
	 * Cannot be combined with other interaction response methods, only the last one will be run.
	 * @param interaction Interaction to use to respond
	 * @return this
	 */
	public ErrorHandler defaultResponse(InteractionBase interaction) {
		responseMsg = DEFAULT_ERROR_MESSAGE;
		this.interaction = interaction;
		return this;
	}

	/**
	 * Responds to the interaction that caused the error with the full error message.
	 * Cannot be combined with other interaction response methods, only the last one will be run.
	 * @param interaction Interaction to use to respond
	 * @return this
	 */
	public ErrorHandler printErrorAsResponse(InteractionBase interaction) {
		responseMsg = getDiscordFormattedError(error);
		this.interaction = interaction;
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
			interaction.createImmediateResponder()
				.setContent(responseMsg)
				.respond();
		}
		if (printToChannelId != null) {
			DiscordUtils.sendMessage(getDiscordFormattedError(error), printToChannelId);
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
