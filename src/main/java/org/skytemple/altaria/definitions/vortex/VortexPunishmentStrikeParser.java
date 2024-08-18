/*
 * Copyright (c) 2023-2024. Frostbyte and other contributors.
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

package org.skytemple.altaria.definitions.vortex;

import org.skytemple.altaria.definitions.ErrorHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class used to parse Vortex punishment messages and extract the number of strikes from them
 */
public class VortexPunishmentStrikeParser {
	private static final Pattern STRIKE_MSG_STRIKE =
		Pattern.compile("\\*\\*(?<moderator>.+)\\*\\*(#\\d+)? (gave|pardoned) `(\\d+)` strikes `\\[.+]` (to|from) " +
			"\\*\\*(?<username>.+)\\*\\*(#\\d+)? \\(ID:(?<id>\\d+)\\)");
	private static final Pattern STRIKE_MSG_OTHER_PUNISHMENT =
		Pattern.compile("\\*\\*(?<moderator>.+)\\*\\*(#\\d+)? (kicked|tempmuted|muted|tempbanned|banned) " +
			"\\*\\*(?<username>.+)\\*\\*(#\\d+)? \\(ID:(?<id>\\d+)\\)");
	@SuppressWarnings("UnnecessaryUnicodeEscape") // Doesn't work otherwise
	private static final Pattern STRIKE_MSG_STRIKE_CHANGE_1 = Pattern.compile("`\\[(\\d+) \u2192 (\\d+)]`");
	@SuppressWarnings("UnnecessaryUnicodeEscape")
	private static final Pattern STRIKE_MSG_STRIKE_CHANGE_2 = Pattern.compile("\\[(\\d+) \u2192 (\\d+) strikes]");

	/**
	 * Given a Discord message containing a Vortex punishment message, returns relevant information about it.
	 * @param message Message to parse
	 * @return Record containing information about the user and the number of strikes after the punishment, or null if
	 * the message couldn't be parsed successfully.
	 */
	public static StrikePunishment parse(String message) {
		Matcher strikesMatcher = STRIKE_MSG_STRIKE.matcher(message);
		Matcher punishmentMatcher = STRIKE_MSG_OTHER_PUNISHMENT.matcher(message);
		Matcher matcher;

		if (strikesMatcher.find()) {
			matcher = strikesMatcher;
		} else if (punishmentMatcher.find()) {
			matcher = punishmentMatcher;
		} else {
			return null;
		}

		try {
			long userId = Long.parseLong(matcher.group("id"));
			String username = matcher.group("username");
			String moderator = matcher.group("moderator");
			int newNumStrikes;

			Matcher strikeChangeMatcher = STRIKE_MSG_STRIKE_CHANGE_1.matcher(message);
			if (strikeChangeMatcher.find()) {
				newNumStrikes = Integer.parseInt(strikeChangeMatcher.group(2));
			} else {
				Matcher strikeChangeMatcher2 = STRIKE_MSG_STRIKE_CHANGE_2.matcher(message);

				if (strikeChangeMatcher2.find()) {
					newNumStrikes = Integer.parseInt(strikeChangeMatcher2.group(2));
				} else {
					return null;
				}
			}
			return new StrikePunishment(userId, username, moderator, newNumStrikes);
		} catch (NumberFormatException e) {
			new ErrorHandler(e).printToErrorChannel().run();
			return null;
		}
	}

	public record StrikePunishment(long userId, String username, String moderator, int newNumStrikes) {}
}
