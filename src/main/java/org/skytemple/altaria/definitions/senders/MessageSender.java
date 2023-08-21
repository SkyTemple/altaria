package org.skytemple.altaria.definitions.senders;

import org.javacord.api.entity.message.embed.EmbedBuilder;

/**
 * Represents an object capable of sending data through Discord messages
 */
public interface MessageSender {
	/**
	 * Sends the specified message
	 * @param message Message to send
	 */
	void send(String message);

	/**
	 * Sends the specified embed
	 * @param embed Embed to send
	 */
	void sendEmbed(EmbedBuilder embed);
}
