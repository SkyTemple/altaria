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

package org.skytemple.altaria.features.support_points;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.message.Message;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.MultiGpList;
import org.skytemple.altaria.definitions.exceptions.AsyncOperationException;
import org.skytemple.altaria.utils.DiscordUtils;
import org.skytemple.altaria.utils.Utils;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletionException;

public abstract class SupportGpCommand implements Command {

	/**
	 * Given a thread, calculates how many GP would be awarded to each user who participated on it. Only messages
	 * between the two specified timestamps will be counted.
	 * @param thread Thread to check
	 * @param startTimestamp Start of the time range to check, in epoch seconds
	 * @param endTimestamp End of the time range to check, in epoch seconds
	 * @return List with the amount of GP each user would get
	 * @throws AsyncOperationException If messages cannot be retrieved for whatever reason
	 */
	protected MultiGpList calcGp(ServerThreadChannel thread, long startTimestamp, long endTimestamp)
		throws AsyncOperationException {
		MultiGpList ret = new MultiGpList("Support thread Guild Points");
		// Number of messages for each user
		Map<Long, Integer> userMessages = new TreeMap<>();

		Set<Message> messages;
		try {
			messages = thread.getMessagesBetween(DiscordUtils.timestampToSnowflake(startTimestamp),
				DiscordUtils.timestampToSnowflake(endTimestamp)).join();
		} catch (CompletionException e) {
			throw new AsyncOperationException(e);
		}

		// Consecutive messages from the same user are counted as one
		long lastUserId = -1;
		for (Message message : messages) {
			Long authorId = message.getUserAuthor().map(DiscordEntity::getId).orElse(null);
			if (authorId != null && authorId != lastUserId) {
				userMessages.merge(authorId, 1, Integer::sum);
				lastUserId = authorId;
			}
		}
		// TODO: Get the list of user overrides for the current thread. Drop users marked as "shouldn't get GP here"
		//  from the list. If OP is listed as "should get GP", don't remove them.
		userMessages.remove(thread.getOwnerId());

		int threadMessages = thread.getMessageCount();
		for (Map.Entry<Long, Integer> entry : userMessages.entrySet()) {
			ret.add(entry.getKey(), calcGp(entry.getValue(), threadMessages));
		}
		return ret;
	}

	/**
	 * Given the number of messages posted by a user on a thread and the total number of messages in it
	 * @param userMessages Number of messages the user sent on the thread
	 * @param threadMessages Total number of messages on the thread
	 * @return Amount of GP the user should get
	 */
	private double calcGp(int userMessages, int threadMessages) {
		Logger logger = Utils.getLogger(getClass(), Level.TRACE);
		logger.trace("userMessages: " + userMessages + ", threadMessages: " + threadMessages);
		// The formula is based on 3 core ideas:
		// 1) More messages yield logarithmically more GP
		// This base amount is only used to calculate the penalty factor, since it depends on how many points the
		// user would get.
		double baseGp = Utils.log(3, userMessages * 0.4 + 1);
		// 2) The more messages on the thread, the less GP
		double basePenaltyFactor = Utils.log(2.1, threadMessages * 0.08 + 1);
		logger.trace("basePenaltyFactor: " + basePenaltyFactor);
		// 3) First points are even harder to get when threads have many messages
		double penaltyFactor = Math.max(1, ((basePenaltyFactor - 1) / Math.pow(2, Math.max(1, baseGp) - 1)) + 1);
		logger.trace("penaltyFactor: " + penaltyFactor);

		// Now we apply the GP formula again, but accounting for penalty
		logger.trace("Result: " + Utils.log(3, userMessages * 0.4 / penaltyFactor + 1));
		return Utils.log(3, userMessages * 0.4 / penaltyFactor + 1);
	}
}
