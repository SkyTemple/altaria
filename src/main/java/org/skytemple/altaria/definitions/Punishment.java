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

package org.skytemple.altaria.definitions;

import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.skytemple.altaria.definitions.enums.PunishmentAction;
import org.skytemple.altaria.definitions.exceptions.AsyncOperationException;
import org.skytemple.altaria.definitions.exceptions.IllegalOperationException;
import org.skytemple.altaria.utils.DurationFormatter;

import java.time.Duration;
import java.util.concurrent.CompletionException;

/**
 * Represents a punishment issued when someone receives a strike
 */
public class Punishment {
	public PunishmentAction action;
	// Null for "none" and "kick" actions, may be null for "ban" action.
	public Duration duration;

	public Punishment(PunishmentAction action, Duration duration) {
		this.action = action;
		this.duration = duration;
	}

	public Punishment(PunishmentAction action, Long durationInSeconds) {
		this.action = action;
		if (durationInSeconds == null) {
			duration = null;
		} else {
			duration = Duration.ofSeconds(durationInSeconds);
		}
	}

	/**
	 * @return The action (and duration if applicable) of this punishment, in a user-readable format
	 */
	public String toString() {
		String actionStr = action.toString().toLowerCase();
		actionStr = Character.toTitleCase(actionStr.charAt(0)) + actionStr.substring(1);
		if (duration == null) {
			return actionStr;
		} else {
			return actionStr + " " + new DurationFormatter(duration).toUserFormat();
		}
	}

	/**
	 * Applies the punishment to a user
	 * @param server Server where the punishment will be applied
	 * @param user User to apply the punishment to
	 * @param reason Audit log reason
	 * @throws AsyncOperationException If the punishment cannot be applied
	 */
	public void apply(Server server, User user, String reason) throws AsyncOperationException {
		switch (action) {
			case NONE:
				break;
			case KICK:
				try {
					server.kickUser(user, reason).join();
				} catch (CompletionException e) {
					throw new AsyncOperationException(e);
				}
				break;
			case MUTE:
				try {
					user.timeout(server, duration, reason).join();
				} catch (CompletionException e) {
					throw new AsyncOperationException(e);
				}
				break;
			case BAN:
				// When banning, we have to inform the user of the duration of the ban so they know if it's temporary
				// or permanent.
				if (duration == null) {
					// Ignore errors on purpose, they might happen because the user doesn't allow DMs.
					// The operation is perfomed synchronously since we can't DM the user after they are banned,
					// so this has to go first.
					user.sendMessage("As a result of your strike, you have been permanently banned from **" +
						server.getName() + "**.").join();
					try {
						server.banUser(user, Duration.ZERO, reason).join();
					} catch (CompletionException e) {
						throw new AsyncOperationException(e);
					}
				} else {
					// Turns out Vortex ignores bot messages so there's no way to tempban someone
					throw new IllegalOperationException("Tempbans not implemented");

					// TODO: Wait until Discord/Vortex supports tempbanning, or manually implement tempbans
					//  on Altaria.
					/*
					DurationFormatter formatter = new DurationFormatter(duration);

					user.sendMessage("As a result of your strike, you have been temporarily banned from **" +
						server.getName() + "** for " + formatter.toUserFormat() + ".").join();

					// We need to ban through Vortex so the ban is temporary
					TextChannel channel = server.getChannelById(ExtConfig.get().getBanCmdChannelId())
						.flatMap(Channel::asTextChannel).orElse(null);
					if (channel != null) {
						channel.sendMessage(">>silentban " + user.getId() + " " + formatter.toVortexFormat() + " " +
							reason);
					} else {
						throw new AsyncOperationException("Set ban channel does not exist or is not a text channel");
					}*/
				}
		}
	}
}
