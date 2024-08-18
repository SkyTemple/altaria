/*
 * Copyright (c) 2023-2024. Frostbyte and other contributors.
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

package org.skytemple.altaria.definitions.senders;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.utils.DiscordUtils;
import org.skytemple.altaria.utils.Utils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Used to send a message to a channel.
 * The channel must be a text channel, must be viewable by the bot, and the bot must have permission to send
 * messages there.
 */
public class ChannelMsgSender extends MessageSender {
	private final long channelId;
	private final Logger logger;
	private final MessageBuilder message;

	/**
	 * @param channelId ID of the channel where the message should be sent
	 */
	public ChannelMsgSender(long channelId) {
		this.channelId = channelId;
		logger = Utils.getLogger(getClass());
		message = new MessageBuilder();
	}

	@Override
	public ChannelMsgSender setText(String text) {
		message.setContent(text);
		return this;
	}

	@Override
	public ChannelMsgSender addEmbed(EmbedBuilder embed) {
		message.addEmbed(embed);
		return this;
	}

	@Override
	public MessageSender addComponent(HighLevelComponent component) {
		message.addComponents(component);
		return this;
	}

	@Override
	public MessageSender addAttachment(byte[] bytes, String filename) {
		message.addAttachment(bytes, filename);
		return this;
	}

	/**
	 * Sets this message to be a reply of the specified message
	 * @param messageToReplyTo Message to reply to
	 * @return this
	 */
	public MessageSender replyTo(Message messageToReplyTo) {
		message.replyTo(messageToReplyTo);
		return this;
	}

	@Override
	public void send() {
		TextChannel channel = tryGetTextChannel();
		if (channel != null) {
			message.send(channel).exceptionally(error -> {
				logger.warn("Failed to send message in channel " + DiscordUtils.getFormattedName(channel) +
					". Error:\n " + Utils.throwableToStr(error));
				return null;
			});
		}
	}

	/**
	 * Attempts to get the {@link TextChannel} associated to this instance.
	 * The resulting object is not stored as an attribute, since its properties might change in the future.
	 * @return The {@link TextChannel} associated to this instance, or null if it couldn't be retrieved due to
	 * an error.
	 */
	private TextChannel tryGetTextChannel() {
		DiscordApi api = ApiGetter.get();
		AtomicReference<TextChannel> result = new AtomicReference<>();

		api.getChannelById(channelId).ifPresentOrElse(channel -> {
			channel.asTextChannel().ifPresentOrElse(result::set, () -> {
				logger.warn("Channel " + DiscordUtils.getFormattedName(channel) + " is not a text channel");
			});
		}, () -> {
			logger.warn("Channel " + channelId + " does not exist");
		});

		return result.get();
	}
}
