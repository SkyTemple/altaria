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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.server.ArchivedThreads;
import org.javacord.api.entity.server.Server;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.exceptions.AsyncOperationException;
import org.skytemple.altaria.definitions.singletons.ExtConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Convenience methods and workarounds for Javacord operations
 */
public class JavacordUtils {
	// Number of threads to get in a row when retrieving archived threads from a channel
	private static final int GET_THREADS_BATCH = 50;
	private static final Object updateThreadMessagesLock = new Object();

	/**
	 * Given a thread and an amount of messages posted, increases the total message count on the thread by that amount.
	 * This method is thread-safe.
	 * @param thread Thread whose message count should be updated
	 * @param messageCountDelta Amount of messages to increase the count by. Supports negative numbers.
	 */
	public static void updateThreadMessageCount(ServerThreadChannel thread, int messageCountDelta) {
		//noinspection OverlyBroadCatchBlock
		try {
			synchronized (updateThreadMessagesLock) {
				int newMessageCount = thread.getMessageCount() + messageCountDelta;
				// Suggestions welcome
				Object threadImpl = Class.forName("org.javacord.core.entity.channel.ServerThreadChannelImpl").cast(thread);
				threadImpl.getClass().getMethod("setMessageCount", int.class).invoke(threadImpl, newMessageCount);
			}
		} catch (ReflectiveOperationException e) {
			new ErrorHandler(e).printToErrorChannel().run();
		}
	}

	/**
	 * Given a thread, returns the ID of the last message posted on it.
	 * If that information is not cached on the bot, a request will be made to obtain it, unlike when calling
	 * {@link ServerThreadChannel#getLastMessageId()}, which just returns 0 in that case.
	 * @param thread Thread to check
	 * @return ID of the last message on the specified thread, or an empty Optional if the thread does not contain
	 * any messages.
	 */
	public static CompletableFuture<Optional<Long>> getLastMessageId(ServerThreadChannel thread) {
		long result = thread.getLastMessageId();
		if (result == 0) {
			return thread.getMessages(1).thenApply(messages -> messages.getNewestMessage().map(DiscordEntity::getId));
		} else {
			return CompletableFuture.completedFuture(Optional.of(result));
		}
	}

	/**
	 * Given a channel, gets all the public threads in it that have at least one message between the two
	 * specified timestamps.
	 * @param channel Channel whose threads should be checed
	 * @param startTime Start time, in epoch seconds
	 * @param endTime End time, in epoch seconds
	 * @return List of public threads in that channel, archived or not.
	 * @throws AsyncOperationException If any of the API requests performed in this method fails for whatever reason.
	 */
	public static List<ServerThreadChannel> getPublicThreadsBetween(ServerTextChannel channel, long startTime,
		long endTime) throws AsyncOperationException {
		Server server = ExtConfig.get().getServer();
		List<ServerThreadChannel> ret = new ArrayList<>();
		Logger logger = Utils.getLogger(DiscordUtils.class, Level.TRACE); // TODO: Remove

		// Get public threads from the server (they can't be obtained from a channel directly)
		List<ServerThreadChannel> serverThreads;
		try {
			serverThreads = server.getActiveThreads().join().getServerThreadChannels();
		} catch (CompletionException e) {
			throw new AsyncOperationException(e);
		}
		for (ServerThreadChannel thread : serverThreads) {
			// Keep only the ones in the channel we care about
			if (thread.getParent().getId() == channel.getId()) {
				// Keep only the ones with messages between the specified time range
				if (thread.getCreationTimestamp().getEpochSecond() < endTime) {
					Long lastMessageId;
					try {
						lastMessageId = getLastMessageId(thread).get().orElse(null);
					} catch (ExecutionException | InterruptedException e) {
						throw new AsyncOperationException(e);
					}
					if (lastMessageId != null && DiscordUtils.snowflakeToTimestamp(lastMessageId) > startTime) {
						ret.add(thread);
						logger.trace("Get thread: " + thread.getName() + " (OPEN)");
					}
				}
			}
		}

		// Get archived public threads from the channel
		long getThreadsBefore = System.currentTimeMillis() / 1000;
		boolean done = false;
		while (!done) {
			List<ServerThreadChannel> currentThreads;
			try {
				// The API docs say the first parameter here is a timestamp used to get threads that were archived
				// before that date, not a thread ID.
				ArchivedThreads requestResult =
					channel.getPublicArchivedThreads(getThreadsBefore, GET_THREADS_BATCH).join();
				done = !requestResult.hasMoreThreads();
				currentThreads = requestResult.getServerThreadChannels();
			} catch (CompletionException e) {
				throw new AsyncOperationException(e);
			}
			for (ServerThreadChannel thread : currentThreads) {
				getThreadsBefore = thread.getId();

				// TODO: Remove after testing with a channel with many archived threads
				if (ret.contains(thread)) {
					done = true;
					logger.warn("Received duplicate thread \"" + thread.getName() + "\" when looping private threads! " +
						"Further threads will be skipped.");
					break;
				}

				if (thread.getMetadata().getArchiveTimestamp().getEpochSecond() >= startTime) {
					long threadCreationTime = DiscordUtils.snowflakeToTimestamp(thread.getId());
					if (threadCreationTime < endTime) {
						Long lastMessageId;
						try {
							lastMessageId = getLastMessageId(thread).get().orElse(null);
						} catch (ExecutionException | InterruptedException e) {
							throw new AsyncOperationException(e);
						}
						if (lastMessageId != null && DiscordUtils.snowflakeToTimestamp(lastMessageId) > startTime) {
							ret.add(thread);
							logger.trace("Get thread: " + thread.getName() + " (ARCHIVED)");
						}
					}
				} else {
					done = true;
					break;
				}
			}
		}

		return ret;
	}
}
