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

import org.javacord.api.entity.Attachment;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.ErrorHandler;
import org.skytemple.altaria.definitions.exceptions.AsyncOperationException;
import org.skytemple.altaria.definitions.exceptions.FileDataParseException;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class RoleColorLoadCommand implements Command {
	private final Server server;
	private final Attachment inputFile;
	private final MessageSender resultSender;
	private final MessageSender errorSender;

	/**
	 * Loads a previously dumped list of role colors and updates role colors in the server accordingly.
	 * @param server Server whose role colors should be loaded
	 * @param inputFile CSV file containing the mapping between role IDs and colors. Should have the same format as the
	 *                  files created by {@link RoleColorDumpCommand}.
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public RoleColorLoadCommand(Server server, Attachment inputFile, MessageSender resultSender, MessageSender errorSender) {
		this.server = server;
		this.inputFile = inputFile;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
	}

	@Override
	public void run() {
		Map<Long, Color> colorMap;
		try {
			colorMap = getRoleColorMap();
		} catch (AsyncOperationException e) {
			new ErrorHandler(e).printToErrorChannel().sendMessage("Error: Could not read contents of given file.",
				errorSender);
			return;
		} catch (FileDataParseException e) {
			new ErrorHandler(e).printToErrorChannel().sendMessage("Error: Invalid input file: " + e.getMessage(),
				errorSender);
			return;
		}

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (Role role : server.getRoles()) {
			Color savedColor = colorMap.get(role.getId());
			Color currentColor = role.getColor().orElse(null);

			if (!Objects.equals(currentColor, savedColor)) {
				if (savedColor == null) {
					// Role did not have a color when the dump was created, reset it
					futures.add(role.updateColor(new Color(0, 0, 0)));
				} else {
					futures.add(role.updateColor(savedColor));
				}
			}
		}

		try {
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		} catch (CancellationException | CompletionException ignore) {}

		long failureCount = futures.stream().filter(CompletableFuture::isCompletedExceptionally).count();
		long successCount = futures.size() - failureCount;

		if (failureCount == 0) {
			if (successCount == 0) {
				resultSender.send("No roles were updated as all of them were unchanged.");
			} else {
				resultSender.send("Successfully updated the color of " + successCount + " roles.");
			}
		} else {
			if (successCount == 0) {
				resultSender.send("Error: Failed to update the color of any of the " + failureCount + " roles.");
			} else {
				resultSender.send("Warning: Successfully updated the color of " + successCount + " roles, but failed " +
					"to update " + failureCount + " roles.");
			}
		}
	}

	/**
	 * Parses the data passed to the command and returns a mapping from role IDs to color
	 * @return Map that maps role IDs to their new color
	 * @throws AsyncOperationException If the given input file cannot be read
	 * @throws FileDataParseException If the data read from the role color dump file is invalid
	 */
	private Map<Long, Color> getRoleColorMap() throws AsyncOperationException, FileDataParseException {
		String fileContents;
		try {
			fileContents = new String(inputFile.asByteArray().get(), StandardCharsets.UTF_8);
		} catch (InterruptedException | ExecutionException e) {
			throw new AsyncOperationException(e);
		}

		Map<Long, Color> result = new TreeMap<>();

		for (String line : fileContents.split("\n")) {
			if (line.isEmpty()) {
				continue;
			}

			String[] lineParts = line.split(",");

			if (lineParts.length != 3) {
				throw new FileDataParseException("Invalid CSV line: " + line);
			}

			long roleId;
			try {
				roleId = Long.parseLong(lineParts[0]);
			} catch (NumberFormatException ignored) {
				throw new FileDataParseException("Invalid role ID: " + lineParts[0]);
			}

			Color roleColor;
			try {
				roleColor = Color.decode(lineParts[2]);
			} catch (NumberFormatException ignored) {
				throw new FileDataParseException("Invalid color: " + lineParts[2]);
			}

			result.put(roleId, roleColor);
		}

		return result;
	}
}
