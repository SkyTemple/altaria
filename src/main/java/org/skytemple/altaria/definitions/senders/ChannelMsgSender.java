package org.skytemple.altaria.definitions.senders;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.utils.DiscordUtils;
import org.skytemple.altaria.utils.Utils;

/**
 * Used to send a message to a channel.
 * The channel must be a text channel, must be viewable by the bot, and the bot must have permission to send
 * messages there.
 */
public class ChannelMsgSender implements MessageSender {
	private final long channelId;

	/**
	 * @param channelId ID of the channel where the message should be sent
	 */
	public ChannelMsgSender(long channelId) {
		this.channelId = channelId;
	}

	@Override
	public void send(String message) {
		DiscordApi api = ApiGetter.get();
		Logger logger = Utils.getLogger(getClass());

		api.getChannelById(channelId).ifPresentOrElse(channel -> {
			channel.asTextChannel().ifPresentOrElse(textChannel -> {
				textChannel.sendMessage(message).exceptionally(error -> {
					logger.warn("Failed to send message in channel " + DiscordUtils.getFormattedName(channel) +
						". Error:\n " + Utils.throwableToStr(error));
					return null;
				});
			}, () -> {
				logger.warn("Channel " + DiscordUtils.getFormattedName(channel) + " is not a text channel");
			});
		}, () -> {
			logger.warn("Channel " + channelId + " does not exist");
		});
	}
}
