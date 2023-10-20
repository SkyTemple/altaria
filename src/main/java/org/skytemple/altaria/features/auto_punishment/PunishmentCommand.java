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

import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.Punishment;
import org.skytemple.altaria.definitions.db.AutoPunishmentDB;
import org.skytemple.altaria.definitions.enums.PunishmentAction;
import org.skytemple.altaria.definitions.exceptions.DbOperationException;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.time.Duration;

public class PunishmentCommand implements Command {
	protected AutoPunishmentDB db;
	protected int strikes;
	protected PunishmentAction action;
	protected Duration duration;
	protected MessageSender resultSender;
	protected MessageSender errorSender;

	public PunishmentCommand(AutoPunishmentDB db, int strikes, PunishmentAction action, Duration duration,
		MessageSender resultSender, MessageSender errorSender) {
		this.db = db;
		this.strikes = strikes;
		this.action = action;
		this.duration = duration;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
	}

	@Override
	public void run() {
		try {
			db.set(strikes, new Punishment(action, duration));
		} catch (DbOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel().run();
			return;
		}
		if (action == PunishmentAction.NONE) {
			resultSender.send("No action will be taken on " + strikes + " strikes.");
		} else {
			resultSender.send("Members will now be " + action.getPastTense() + " upon reaching " + strikes + " strikes.");
		}
	}
}
