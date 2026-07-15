package org.skytemple.altaria.features.verification;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.utils.Utils;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionException;

/**
 * Gives members who post a certain amount of messages since the last time the bot was restarted a verified user role
 */
public class Verification {
	private final DiscordApi api;
	private final ExtConfig extConfig;
	private final Logger logger;

	// ID of the server where the verification role will be given to users
	private final long serverId;
	// IF of the role given to verified users, or null if the feature is disabled
	private final Long verifiedRoleId;
	// Number of messages a user has to post to be considered verified
	private final Integer requiredPosts;

	// Used to store the number of messages sent by each unverified user since the bot started
	private final Map<Long, Integer> messageCounts;

	public Verification() {
		api = ApiGetter.get();
		extConfig = ExtConfig.get();
		logger = Utils.getLogger(getClass());

		serverId = extConfig.getServer().getId();
		Long roleId = extConfig.getVerifiedUserRoleId();
		requiredPosts = extConfig.getVerifiedMessageThreshold();

		messageCounts = new TreeMap<>();

		if (roleId != null && requiredPosts != null) {
			Role verifiedRole = api.getRoleById(roleId).orElse(null);
			if (verifiedRole == null) {
				logger.error("Cannot find verified user role (ID " + roleId + "). Feature will be disabled.");
				verifiedRoleId = null;
				return;
			}

			verifiedRoleId = roleId;
			api.addMessageCreateListener(this::handleMessage);
		} else {
			verifiedRoleId = null;
		}
	}

	/**
	 * Triggered when a new message is posted. If the user who posted it is not verified and their new total message
	 * count reaches the required threshold, gives them the verified user role.
	 *
	 * @param event Messsage creation event
	 */
	private void handleMessage(MessageCreateEvent event) {
		MessageAuthor author = event.getMessageAuthor();

		if (author.isBotUser() || author.isWebhook()) {
			// Ignore
			return;
		}

		User user = author.asUser().orElse(null);
		Server server = event.getServer().orElse(null);
		if (user == null || server == null) {
			return;
		}

		if (server.getId() != serverId) {
			// Message is from another server, ignore
			return;
		}

		synchronized (this) {
			if (!user.getRoles(server).stream().map(DiscordEntity::getId).toList().contains(verifiedRoleId)) {
				int messageCount = messageCounts.getOrDefault(user.getId(), 0) + 1;

				if (messageCount >= requiredPosts) {
					// User has enough messages to be verified, give them the role and remove them from the map
					try {
						Role verifiedRole = api.getRoleById(verifiedRoleId).orElse(null);
						if (verifiedRole == null) {
							logger.error("Cannot find verified user role (ID " + verifiedRoleId + "). " +
								"User " + user.getId() + " (" + user.getName() + ") will remain unverified.");
							return;
						}

						user.addRole(verifiedRole).join();
						messageCounts.remove(user.getId());
						logger.info("User " + user.getId() + " (" + user.getName() + ") passed verification");
					} catch (CompletionException e) {
						new ErrorHandler(e).printToErrorChannel().run();
					}
				} else {
					// Not enough messages yet, store the new count
					messageCounts.put(user.getId(), messageCount);
				}
			}
		}
	}
}
