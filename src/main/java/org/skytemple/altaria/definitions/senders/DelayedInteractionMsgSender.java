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

package org.skytemple.altaria.definitions.senders;

import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.InteractionBase;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.skytemple.altaria.definitions.exceptions.IllegalOperationException;
import org.skytemple.altaria.utils.Utils;

import java.util.concurrent.CompletableFuture;

/**
 * Used to send a message in response to an interaction after a certain amount of time. Allows for up to 15 minutes
 * of delay.
 */
public class DelayedInteractionMsgSender extends InteractionMsgSender {
	private final CompletableFuture<InteractionOriginalResponseUpdater> response;
	private boolean ephemeral;
	private final Logger logger;

	/**
	 * Creates a new delayed interaction message sender.
	 * WARNING: This sends a respond later message to Discord!
	 * Do not instantiate this class unless you intend to respond!
	 * @param interaction Interaction used to send the message as an interaction response
	 * @param ephemeral True to respond with an ephemeral message
	 */
	public DelayedInteractionMsgSender(InteractionBase interaction, boolean ephemeral) {
		logger = Utils.getLogger(getClass());

		logger.debug("Sending respond later interaction response");
		response = interaction.respondLater(ephemeral);
		this.ephemeral = ephemeral;
	}

	@Override
	public DelayedInteractionMsgSender setText(String text) {
		response.join().setContent(text);
		return this;
	}

	@Override
	public DelayedInteractionMsgSender addEmbed(EmbedBuilder embed) {
		response.join().addEmbed(embed);
		return this;
	}

	@Override
	public DelayedInteractionMsgSender addComponent(HighLevelComponent component) {
		response.join().addComponents(component);
		return this;
	}

	@Override
	public DelayedInteractionMsgSender addAttachment(byte[] bytes, String filename) {
		response.join().addAttachment(bytes, filename);
		return this;
	}

	/**
	 * Since this class requires that the ephemeral status is set when instantiating it, this method doesn't actually
	 * change the ephemeral status.
	 * If this method is called when the message wasn't set as ephemeral when instantiating it, throws
	 * {@link org.skytemple.altaria.definitions.exceptions.IllegalOperationException}.
	 * @return This
	 */
	@Override
	public DelayedInteractionMsgSender setEphemeral() {
		if (!ephemeral) {
			throw new IllegalOperationException("Cannot set a DelayedInteractionMessageSender as ephemeral after " +
				"instantiating it. Set the flag on the constructor instead.");
		}
		return this;
	}

	@Override
	public void send() {
		response.join().update();
	}
}
