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

package org.skytemple.altaria.features.auto_punishment;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.db.AutoPunishmentDB;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.util.List;

public class PunishmentsCommand implements Command {
	protected AutoPunishmentDB db;
	protected MessageSender resultSender;
	protected MessageSender errorSender;

	public PunishmentsCommand(AutoPunishmentDB db, MessageSender resultSender, MessageSender errorSender) {
		this.db = db;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
	}

	@Override
	public void run() {
		try {
			List<AutoPunishmentDB.StrikesAndPunishment> punishments = db.getAll();
			StringBuilder descriptionBuilder = new StringBuilder();
			for (int i = 0; i < punishments.size(); i++) {
				AutoPunishmentDB.StrikesAndPunishment entry = punishments.get(i);
				// \uD83D\uDEA9 = 🚩
				descriptionBuilder.append("`").append(entry.strikes()).append(" \uD83D\uDEA9`: ")
					.append(entry.punishment().toString());
				if (i < punishments.size() - 1) {
					descriptionBuilder.append("\n");
				}
			}
			EmbedBuilder embed = new EmbedBuilder()
				.setTitle("Strike punishments")
				.setDescription(descriptionBuilder.toString());
			resultSender.addEmbed(embed).send();
		} catch (DbOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
		}
	}
}
