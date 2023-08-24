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

import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.embed.EmbedBuilder;

/**
 * Represents an object capable of sending data through Discord messages
 */
public abstract class MessageSender {
	/**
	 * Sets the text that will be sent alongside the message. Overwrites previously added text.
	 * @param text Text to send
	 * @return this
	 */
	public abstract MessageSender setText(String text);

	/**
	 * Adds an embed to the message that will be sent
	 * @param embed Embed to send
	 * @return this
	 */
	public abstract MessageSender addEmbed(EmbedBuilder embed);

	/**
	 * Adds a component to the message that will be sent
	 * @param component Component to add
	 * @return this
	 */
	public abstract MessageSender addComponent(HighLevelComponent component);

	/**
	 * Sends the composed message
	 */
	public abstract void send();

	/**
	 * Convenience method that directly sends a message
	 */
	public void send(String text) {
		setText(text);
		send();
	}
}
