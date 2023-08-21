package org.skytemple.altaria.definitions.message_sender;

import org.javacord.api.interaction.InteractionBase;

/**
 * Used to send a message in response to an interaction
 */
public class InteractionMsgSender implements MessageSender {
	private final InteractionBase interaction;

	/**
	 * @param interaction Interaction used to send the message as an interaction response
	 */
	public InteractionMsgSender(InteractionBase interaction) {
		this.interaction = interaction;
	}

	@Override
	public void send(String message) {
		interaction.createImmediateResponder().setContent(message).respond();
	}
}
