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

import org.javacord.api.entity.channel.ServerThreadChannel;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.MultiGpList;
import org.skytemple.altaria.definitions.exceptions.AsyncOperationException;
import org.skytemple.altaria.definitions.senders.MessageSender;

public class SupportGpCheckCommand extends SupportGpCommand {
	private final ServerThreadChannel thread;
	private final MessageSender resultSender;
	private final MessageSender errorSender;

	/**
	 * Given a thread, determines how many points the users on it should receive. All messages in the thread will
	 * be counted.
	 * @param thread Thread to check
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public SupportGpCheckCommand(ServerThreadChannel thread, MessageSender resultSender, MessageSender errorSender) {
		this.thread = thread;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
	}

	@Override
	public void run() {
		MultiGpList list;
		try {
			list = calcGp(thread, 0L, System.currentTimeMillis() / 1000);
		} catch (AsyncOperationException e) {
			new ErrorHandler(e).printToErrorChannel().sendDefaultMessage(errorSender).run();
			return;
		}
		resultSender.addEmbed(list.toEmbed(false)).send();
	}
}
