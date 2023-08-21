package org.skytemple.altaria.definitions.senders;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
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
public class ChannelMsgSender implements MessageSender {
	private final long channelId;
	private final Logger logger;

	/**
	 * @param channelId ID of the channel where the message should be sent
	 */
	public ChannelMsgSender(long channelId) {
		this.channelId = channelId;
		logger = Utils.getLogger(getClass());
	}

	@Override
	public void send(String message) {
		TextChannel channel = tryGetTextChannel();
		if (channel != null) {
			channel.sendMessage(message).exceptionally(error -> {
				logger.warn("Failed to send message in channel " + DiscordUtils.getFormattedName(channel) +
					". Error:\n " + Utils.throwableToStr(error));
				return null;
			});
		}
	}

	@Override
	public void sendEmbed(EmbedBuilder embed) {
		TextChannel channel = tryGetTextChannel();
		if (channel != null) {
			channel.sendMessage(embed).exceptionally(error -> {
				logger.warn("Failed to send embed in channel " + DiscordUtils.getFormattedName(channel) +
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
