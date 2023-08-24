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

package org.skytemple.altaria.definitions.senders;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.InteractionBase;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

/**
 * Used to send a message in response to an interaction
 */
public class InteractionMsgSender extends MessageSender {
	private final InteractionBase interaction;
	private final InteractionImmediateResponseBuilder response;

	/**
	 * @param interaction Interaction used to send the message as an interaction response
	 */
	public InteractionMsgSender(InteractionBase interaction) {
		this.interaction = interaction;
		response = interaction.createImmediateResponder();
	}

	@Override
	public InteractionMsgSender setText(String text) {
		response.setContent(text);
		return this;
	}

	@Override
	public InteractionMsgSender addEmbed(EmbedBuilder embed) {
		response.addEmbed(embed);
		return this;
	}

	@Override
	public MessageSender addComponent(HighLevelComponent component) {
		response.addComponents(component);
		return this;
	}

	/**
	 * Turns the response into an ephemeral message
	 * @return this
	 */
	public MessageSender setEphemeral() {
		response.setFlags(MessageFlag.EPHEMERAL);
		return this;
	}

	@Override
	public void send() {
		response.respond();
	}
}
