/*
 * Copyright (c) 2023-2024. End45 and other contributors.
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

package org.skytemple.altaria.features.auto_punishment;

import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.utils.Utils;

import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class used to parse Vortex strike messages
 */
public class VortexStrikeParser {
	private static final Pattern STRIKE_MSG_STRIKES = Pattern.compile("gave `(\\d+)` strikes");
	private static final Pattern STRIKE_MSG_USER_ID = Pattern.compile("\\(ID:(\\d+)\\)");
	@SuppressWarnings("UnnecessaryUnicodeEscape") // Doesn't work otherwise
	private static final Pattern STRIKE_MSG_STRIKE_CHANGE = Pattern.compile("`\\[(\\d+) \u2192 (\\d+)]`");
	private static final Pattern STRIKE_MSG_REASON = Pattern.compile("`\\[ Reason ]` (.*)");

	/**
	 * Given a Discord message containing a Vortex strike, returns relevant information about it.
	 * @param message Message to parse
	 * @return Record containing information about the strike, or null if the message couldn't be parsed successfully.
	 */
	public static Strike parse(String message) {
		DiscordApi api = ApiGetter.get();
		Logger logger = Utils.getLogger(VortexStrikeParser.class);

		Matcher strikesMatcher = STRIKE_MSG_STRIKES.matcher(message);
		if (strikesMatcher.find()) {
			try {
				int strikesGiven = Integer.parseInt(strikesMatcher.group(1));
				long userId;
				int oldNumStrikes;
				int newNumStrikes;
				String reason;

				Matcher userIdMatcher = STRIKE_MSG_USER_ID.matcher(message);
				if (userIdMatcher.find()) {
					userId = Long.parseLong(userIdMatcher.group(1));
				} else {
					new ErrorHandler(new IllegalStateException("Cannot find user ID in strike message. " +
						"Message:\n" + message)).printToErrorChannel().run();
					return null;
				}
				User user;
				try {
					user = api.getUserById(userId).join();
				} catch (CompletionException e) {
					logger.error("Cannot find user to timeout. User ID: " + userId);
					new ErrorHandler(e).printToErrorChannel().run();
					return null;
				}

				Matcher strikeChangeMatcher = STRIKE_MSG_STRIKE_CHANGE.matcher(message);
				if (strikeChangeMatcher.find()) {
					oldNumStrikes = Integer.parseInt(strikeChangeMatcher.group(1));
					newNumStrikes = Integer.parseInt(strikeChangeMatcher.group(2));
				} else {
					new ErrorHandler(new IllegalStateException("Cannot find strike change in strike message. " +
						"Message:\n" + message)).printToErrorChannel().run();
					return null;
				}

				Matcher reasonMatcher = STRIKE_MSG_REASON.matcher(message);
				if (reasonMatcher.find()) {
					reason = reasonMatcher.group(1);
				} else {
					new ErrorHandler(new IllegalStateException("Cannot find strike reason in strike message. " +
						"Message:\n" + message)).printToErrorChannel().run();
					return null;
				}

				logger.debug("New strike detected. Strikes given: " + strikesGiven + ", Number of strikes: " +
					oldNumStrikes + " -> " + newNumStrikes + ", reason: " + reason);
				return new Strike(strikesGiven, oldNumStrikes, newNumStrikes, user, reason);
			} catch (NumberFormatException e) {
				new ErrorHandler(e).printToErrorChannel().run();
				return null;
			}
		} else {
			return null;
		}
	}

	public record Strike(int strikesGiven, int oldNumStrikes, int newNumStrikes, User user, String reason) {}
}
