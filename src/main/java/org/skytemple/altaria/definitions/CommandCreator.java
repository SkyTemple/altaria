/*
 * Copyright (c) 2024. End45 and other contributors.
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

package org.skytemple.altaria.definitions;

import org.javacord.api.DiscordApi;
import org.javacord.api.interaction.ApplicationCommandBuilder;
import org.skytemple.altaria.definitions.exceptions.IllegalOperationException;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Class used to create and register slash commands in bulk.
 */
public class CommandCreator implements AutoCloseable {
	private final DiscordApi api;
	private final ExtConfig extConfig;
	private final List<ApplicationCommandBuilder<?, ?, ?>> commands;
	private boolean closed;

	/**
	 * Creates a new CommandCreator. It can then be used to register application commands (both slash commands and
	 * interactions) that will be submitted to Discord's API once the {@link #close()} method is called.
	 * Instances of this class are not reusable.
	 */
	public CommandCreator() {
		api = ApiGetter.get();
		extConfig = ExtConfig.get();
		commands = new ArrayList<>();
		closed = false;
	}

	/**
	 * Registers a new slash command. The command will be submitted to Discord's API once the {@link #close()} method
	 * is called.
	 * This method may not be used once the command creator has been closed. Trying to do so will throw an
	 * {@link org.skytemple.altaria.definitions.exceptions.IllegalOperationException}.
	 * @param command Command to register
	 * @throws org.skytemple.altaria.definitions.exceptions.IllegalOperationException If the command creator has
	 * already been closed.
	 */
	public void registerCommand(ApplicationCommandBuilder<?, ?, ?> command) {
		if (closed) {
			throw new IllegalOperationException("Cannot register any more commands after a command creator " +
				"has been closed.");
		}
		commands.add(command);
	}

	/**
	 * Closes the command creator. All commands registered so far will be submitted to Discord's API. Sets the
	 * internal state of the creator to closed.
	 */
	@Override
	public void close() {
		closed = true;
		api.bulkOverwriteServerApplicationCommands(extConfig.getGuildId(), new HashSet<>(commands))
			.exceptionally(e -> {new ErrorHandler(e).printToErrorChannel().run(); return null;})
			.join();
	}
}
