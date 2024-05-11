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

package org.skytemple.altaria.features.mod_actions;

import org.javacord.api.entity.message.Message;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.Constants;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PinMessageCommand implements Command {
	protected Message message;
	protected MessageSender resultSender;
	protected MessageSender errorSender;

	/**
	 * Pins or unpins the specified message
	 * @param message Message to pin/unpin
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public PinMessageCommand(Message message, MessageSender resultSender, MessageSender errorSender) {
		this.message = message;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
	}

	@Override
	public void run() {
		if (message.isPinned()) {
			try {
				message.unpin().get(Constants.ACTION_TIMEOUT, TimeUnit.SECONDS);
				resultSender.send("Succesfully unpinned message.");
			} catch (CompletionException | InterruptedException | ExecutionException e){
				new ErrorHandler(e).sendMessage("Error: Failed to unpin message.", errorSender).printToErrorChannel().run();
			} catch (TimeoutException e) {
				errorSender.send("Error: Unpin operation timed out. The bot might be getting rate-limited.");
			}
		} else {
			try {
				message.pin().get(Constants.ACTION_TIMEOUT, TimeUnit.SECONDS);
				resultSender.send("Successfully pinned message.");
			} catch (CompletionException | InterruptedException | ExecutionException e){
				new ErrorHandler(e).sendMessage("Error: Failed to pin message.", errorSender).printToErrorChannel().run();
			} catch (TimeoutException e) {
				errorSender.send("Error: Pin operation timed out. The bot might be getting rate-limited.");
			}
		}
	}
}
