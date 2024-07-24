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

package org.skytemple.altaria.features.reputation;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.db.ReputationDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.awt.*;
import java.util.List;

public class ListGpCommand implements Command {
	// This field is null if the command was created with a cached leaderboard
	protected ReputationDB rdb;
	protected int page;
	protected MessageSender resultSender;
	protected MessageSender errorSender;

	// Stores the retrieved leaderboard so it can be returned later. Null if the leaderboard hasn't been obtained yet.
	protected Leaderboard leaderboard;

	/**
	 * Gets a part of the GP leaderboard
	 * @param rdb Reputation database instance
	 * @param page Page to get (0-indexed). Negative numbers can be used to refer to the last pages, with -1 being the
	 *             last one.
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public ListGpCommand(ReputationDB rdb, int page, MessageSender resultSender, MessageSender errorSender) {
		this.rdb = rdb;
		this.page = page;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
		leaderboard = null;
	}

	/**
	 * Gets a part of the GP leaderboard. The leaderboard to use is specified here, which allows reusing a previous
	 * result of the command.
	 * @param leaderboard Leaderboard to use to show results
	 * @param page Page to get (0-indexed). Negative numbers can be used to refer to the last pages, with -1 being the
	 *             last one.
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public ListGpCommand(Leaderboard leaderboard, int page, MessageSender resultSender, MessageSender errorSender) {
		rdb = null;
		this.page = page;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
		this.leaderboard = leaderboard;
	}

	@Override
	public void run() {
		if (leaderboard == null) {
			// Get an up-to-date version of the leaderboard
			try {
				List<ReputationDB.PointsEntryInt> entries = rdb.getPointsInt();
				leaderboard = new Leaderboard(entries);
			} catch (DbOperationException e) {
				new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
				return;
			}
		}

		// Current page number to display to the user (1-indexed)
		int displayPage = (page < 0 ? leaderboard.convertNegativePage(page) : page) + 1;
		int numPages = leaderboard.getNumPages();

		try {
			// Send the result in an embed to avoid pinging anyone
			EmbedBuilder embed = new EmbedBuilder()
				.setTitle("Guild Points leaderboard")
				.setDescription(leaderboard.getPage(page))
				.setFooter("Page " + displayPage + "/" + numPages)
				.setColor(Color.YELLOW);
			resultSender.addEmbed(embed).send();
		} catch (IllegalArgumentException e) {
			errorSender.send("Page number out of bounds. Maximum page: " + numPages + ".");
		}
	}

	/**
	 * Returns the leaderboard stored by the instance.
	 * If no leaderboard was provided when the instance was created and the {@link #run()} method hasn't been
	 * successfully run at least once, returns null.
	 * @return Instance leaderboard
	 */
	public Leaderboard getLeaderboard() {
		return leaderboard;
	}
}
