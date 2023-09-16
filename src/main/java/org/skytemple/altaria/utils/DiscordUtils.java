/*
 * Copyright (c) 2023. End45 and other contributors.
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

package org.skytemple.altaria.utils;

import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.Channel;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Convenience methods for operations with the Discord API
 */
public class DiscordUtils {
	// Timestamp in ms used to convert Discord snowflakes. Represents the first second of 2015.
	private static final long DISCORD_EPOCH = 1420070400000L;

	/**
	 * Given a Discord snowflake ID, returns the equivalent timestamp (the time when the object with the specified
	 * snowflake was created)
	 * @param snowflake Snowflake to convert
	 * @return Timestamp associated to the given snowflake, in epoch seconds.
	 */
	public static long snowflakeToTimestamp(long snowflake) {
		return ((snowflake >> 22) + DISCORD_EPOCH) / 1000;
	}

	/**
	 * Given a timestamp, returns a Discord snowflake that would represent an object created at that time.
	 * @param timestamp Timestamp to convert, in epoch seconds. If lower than {@link #DISCORD_EPOCH}, it will be set
	 *                  to that value first.
	 * @return Resulting snowflake
	 */
	public static long timestampToSnowflake(long timestamp) {
		long discord_timestamp = timestamp * 1000 - DISCORD_EPOCH;
		if (discord_timestamp < 0) {
			discord_timestamp = 0;
		}
		return discord_timestamp << 22;
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

	/**
	 * Sends a JSON message through the specified message sender with the result of a command
	 * @param success True if the command was successful
	 * @param resultStr Result string to include in the message
	 */
	public static void sendJsonResult(MessageSender sender, boolean success, String resultStr) {
		String status = success ? "success" : "error";
		sender.send("{\"status\": \"" + status + "\", \"result\": \"" + resultStr + "\"}");
	}
}
