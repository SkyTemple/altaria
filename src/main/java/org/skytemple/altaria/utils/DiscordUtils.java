package org.skytemple.altaria.utils;

import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.Channel;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Convenience methods for operations with the Discord API
 */
public class DiscordUtils {
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
