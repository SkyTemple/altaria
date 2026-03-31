/*
 * Copyright (c) 2023-2026. Frostbyte and other contributors.
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

package org.skytemple.altaria.features.fun.fun2026;

import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.awt.*;
import java.nio.charset.StandardCharsets;

public class RoleColorDumpCommand implements Command {
	private final Server server;
	private final boolean textDump;
	private final MessageSender resultSender;

	/**
	 * Dumps the list of roles of a server that have a custom color to a file, including a hex representation of
	 * the color.
	 * @param server Server whose role colors should be dumped
	 * @param textDump True to dump role colors as text, false to dump as CSV.
	 * @param resultSender Used to send result messages to the user
	 */
	public RoleColorDumpCommand(Server server, boolean textDump, MessageSender resultSender) {
		this.server = server;
		this.textDump = textDump;
		this.resultSender = resultSender;
	}

	@Override
	public void run() {
		StringBuilder sb = new StringBuilder();

		for (Role role : server.getRoles()) {
			Color color = role.getColor().orElse(null);

			if (color != null) {
				String colorHex = String.format("#%06X", color.getRGB() & 0xFFFFFF);

				if (textDump) {
					sb.append(role.getName()).append(": ").append(colorHex).append("\n");
				} else {
					sb.append(role.getId()).append(",").append(role.getName()).append(",").append(colorHex)
						.append("\n");
				}
			}
		}

		String filename = "Role colors." + (textDump ? "txt" : "csv");
		resultSender.addAttachment(sb.toString().getBytes(StandardCharsets.UTF_8), filename);
		resultSender.send();
	}
}
