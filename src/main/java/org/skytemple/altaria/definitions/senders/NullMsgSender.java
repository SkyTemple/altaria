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

import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.embed.EmbedBuilder;

/**
 * Discards incoming messages without sending them anywhere.
 */
public class NullMsgSender extends MessageSender {

	@Override
	public MessageSender setText(String text) {
		return this;
	}

	@Override
	public MessageSender addEmbed(EmbedBuilder embed) {
		return this;
	}

	@Override
	public MessageSender addComponent(HighLevelComponent component) {
		return this;
	}

	@Override
	public MessageSender addAttachment(byte[] bytes, String filename) {
		return this;
	}

	@Override
	public void send() {}
}
