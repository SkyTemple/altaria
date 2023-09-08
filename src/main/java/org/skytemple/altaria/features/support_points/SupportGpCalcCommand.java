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

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.MultiGpList;
import org.skytemple.altaria.definitions.exceptions.AsyncOperationException;
import org.skytemple.altaria.definitions.senders.InteractionMsgSender;
import org.skytemple.altaria.definitions.senders.MessageSender;
import org.skytemple.altaria.utils.JavacordUtils;

import java.util.List;

public class SupportGpCalcCommand extends SupportGpCommand {
	private final ServerTextChannel channel;
	private final long startTimestamp;
	private final long endTimestamp;
	private final InteractionMsgSender resultSender;
	private final MessageSender errorSender;
	private final MultiGpListConsumer gpListConsumer;

	/**
	 * Given a channel and a time range, determines how many points the users who posted on threads in that channel
	 * should receive. Only messages within the specified time range will be counted.
	 * After the command is run, the generated multi-GP list will be printed as an embed. It will also be passed to the
	 * specified GP list consumer.
	 * @param channel Channel where messages will be counted to calculate the GP amounts
	 * @param startTimestamp Start of the time range to check, in epoch seconds
	 * @param endTimestamp End of the time range to check, in epoch seconds
	 * @param resultSender Used to send result messages to the user. Must be an interaction since the result message
	 *                     is ephemeral.
	 * @param errorSender Used to send error messages to the user
	 * @param gpListConsumer Code that will consume the generated multi-GP list
	 */
	public SupportGpCalcCommand(ServerTextChannel channel, long startTimestamp, long endTimestamp,
		InteractionMsgSender resultSender, MessageSender errorSender, MultiGpListConsumer gpListConsumer) {
		this.channel = channel;
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
		this.gpListConsumer = gpListConsumer;
	}

	@Override
	public void run() {
		try {
			List<ServerThreadChannel> threads =
				JavacordUtils.getPublicThreadsBetween(channel, startTimestamp, endTimestamp);

			// Calculate GP for each thread
			MultiGpList gpList = new MultiGpList("Support Guild Points");
			for (ServerThreadChannel thread : threads) {
				gpList.addAll(calcGp(thread, startTimestamp, endTimestamp));
			}
			resultSender.setText("These are the points that will be awarded for support contributions on the specified " +
				"time period. Please confirm if you're okay with them.").addEmbed(gpList.toEmbed(true))
				.addComponent(ActionRow.of(
					Button.success(SupportPoints.COMPONENT_SUPPORT_GP_CONFIRM, "Confirm")
				)).setEphemeral().send();
			gpListConsumer.consume(gpList);
		} catch (AsyncOperationException e) {
			new ErrorHandler(e).sendDefaultMessage(errorSender).printToErrorChannel();
		}
	}

	@FunctionalInterface
	public interface MultiGpListConsumer {
		void consume(MultiGpList gpList);
	}
}
