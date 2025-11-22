package org.skytemple.altaria.features.verification;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
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

    // Server where the verification role will be given to users
    private final Server server;
	// Role given to verified users
	private final Role verifiedRole;
	// Number of messages a user has to post to be considered verified
	private final Integer requiredPosts;

	// Used to store the number of messages sent by each unverified user since the bot started
	private final Map<Long, Integer> messageCounts;

	public Verification() {
		api = ApiGetter.get();
		extConfig = ExtConfig.get();
		logger = Utils.getLogger(getClass());

        server = extConfig.getServer();
		Long roleId = extConfig.getVerifiedUserRoleId();
		requiredPosts = extConfig.getVerifiedMessageThreshold();

		messageCounts = new TreeMap<>();

		if (roleId != null && requiredPosts != null) {
			verifiedRole = api.getRoleById(roleId).orElse(null);
			if (verifiedRole == null) {
				logger.error("Cannot find verified user role (ID " + roleId + "). Feature will be disabled.");
				return;
			}

			api.addMessageCreateListener(this::handleMessage);
		} else {
			verifiedRole = null;
		}
	}

	/**
	 * Triggered when a new message is posted. If the user who posted it is not verified and their new total message
	 * count reaches the required threshold, gives them the verified user role.
	 * @param event Messsage creation event
	 */
	private void handleMessage(MessageCreateEvent event) {
		User user = event.getMessageAuthor().asUser().orElse(null);
		Server server = event.getServer().orElse(null);
		if (user == null || server == null) {
			logger.warn("Ignoring message " + event.getMessageId() + " because author or server fields are null");
			return;
		}
        if (server != this.server) {
            // Message is from another server, ignore
            return;
        }

		if (!user.getRoles(server).contains(verifiedRole)) {
			synchronized (this) {
                int messageCount = messageCounts.getOrDefault(user.getId(), 0) + 1;

                if (messageCount >= requiredPosts) {
                    // User has enough messages to be verified, give them the role and remove them from the map
                    try {
                        user.addRole(verifiedRole).join();
                        messageCounts.remove(user.getId());
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
