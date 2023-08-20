package org.skytemple.altaria;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.skytemple.altaria.singletons.ApiGetter;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Convenience methods for operations with the Discord API
 */
public class DiscordUtils {
	/**
	 * Given a message an the ID of a channel, sends the message to that channel.
	 * The channel must be a text channel, must be viewable by the bot, and the bot must have permission to send
	 * messages there.
	 * @param msg Message to send
	 * @param channelId Channel where the message should be sent
	 */
	public static void sendMessage(String msg, long channelId) {
		DiscordApi api = ApiGetter.get();
		Logger logger = Utils.getLogger(DiscordUtils.class);

		api.getChannelById(channelId).ifPresentOrElse(channel -> {
			channel.asTextChannel().ifPresentOrElse(textChannel -> {
				textChannel.sendMessage(msg).exceptionally(error -> {
					logger.warn("Failed to send message in channel " + getFormattedName(channel) + ". Error:\n " +
						Utils.throwableToStr(error));
					return null;
				});
			}, () -> {
				logger.warn("Channel " + getFormattedName(channel) + " is not a text channel");
			});
		}, () -> {
			logger.warn("Channel " + channelId + " does not exist");
		});
	}

	/**
	 * Returns the formatted name of a channel. The resulting string will specify the name of the channel and
	 * the server it's contained in. If the channel is a DM, it will specify the name of the recipient.
	 * @param channel Channel
	 * @return Formatted name of the channel
	 */
	public static String getFormattedName(Channel channel) {
		Logger logger = Utils.getLogger(DiscordUtils.class);
		AtomicReference<String> res = new AtomicReference<>("<Unknown>");

		channel.asServerChannel().ifPresentOrElse(serverChannel -> {
			res.set(serverChannel.getServer().getName() + "/" + serverChannel.getName());
		}, () -> {
			channel.asPrivateChannel().ifPresentOrElse(privateChannel -> {
				privateChannel.getRecipient().ifPresentOrElse(user -> {
					res.set("DM/" + user.getName());
				}, () -> {
					logger.warn("Private channel with ID " + channel.getId() + " does not have a recipient");
				});
			}, () -> {
				logger.warn("Channel with ID " + channel.getId() + " is not a server channel nor a private channel");
			});
		});
		return res.get();
	}
}
