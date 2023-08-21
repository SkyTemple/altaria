package org.skytemple.altaria.utils;

import org.skytemple.altaria.definitions.message_sender.MessageSender;

/**
 * Class used to specify if and how command feedback (result and error messages) should be sent to the user.
 */
public class FeedbackSender {
	private final MessageSender resultSender;
	private final MessageSender errorSender;

	/**
	 * Instantiates the class, optionally specifying sender to use to print errors and feedback.
	 * @param resultSender Sender used to send result messages. If null, no result messages will be sent.
	 * @param errorSender Sender used to send error messages. If null, no error messages will be sent.
	 */
	protected FeedbackSender(MessageSender resultSender, MessageSender errorSender) {
		this.resultSender = resultSender;
		this.errorSender = errorSender;
	}

	/**
	 * @return FeedbackSender that doesn't provide any feedback to the user.
	 */
	public static FeedbackSender noFeedback() {
		return new FeedbackSender(null, null);
	}

	/**
	 * @param resultSender Sender used to send result messages
	 * @return FeedbackSender that sends result messages, but not error messages.
	 */
	public static FeedbackSender resultsOnly(MessageSender resultSender) {
		return new FeedbackSender(resultSender, null);
	}

	/**
	 * @param errorSender Sender used to send error messages
	 * @return FeedbackSender that sends error messages, but not result messages.
	 */
	public static FeedbackSender errorsOnly(MessageSender errorSender) {
		return new FeedbackSender(null, errorSender);
	}

	/**
	 * @param resultSender Sender used to send result messages
	 * @param errorSender Sender used to send error messages
	 * @return FeedbackSender that sends result messages and error messages
	 */
	public static FeedbackSender fullFeedback(MessageSender resultSender, MessageSender errorSender) {
		return new FeedbackSender(resultSender, errorSender);
	}

	/**
	 * Sends a result message, if this instance was configured to send result messages.
	 * @param message Message to send
	 */
	public void result(String message) {
		if (resultSender != null) {
			resultSender.send(message);
		}
	}

	/**
	 * Sends an error message, if this instance was configured to send error messages.
	 * @param message Message to send
	 */
	public void error(String message) {
		if (errorSender != null) {
			errorSender.send(message);
		}
	}
}
