package org.skytemple.altaria.definitions.senders;

import org.javacord.api.entity.message.embed.EmbedBuilder;

/**
 * Discards incoming messages without sending them anywhere.
 */
public class NullMsgSender implements MessageSender {

	@Override
	public void send(String message) {}

	@Override
	public void sendEmbed(EmbedBuilder embed) {}
}
