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

package org.skytemple.altaria.definitions;

import org.javacord.api.entity.Attachment;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandInteractionOptionsProvider;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class used to retrieve arguments from a Discord command, while also handling the cases where a value might be missing
 * and potential error situations.
 */
public class CommandArgumentList {
	private final SlashCommandInteractionOptionsProvider command;
	// Used to print errors
	private final MessageSender errorMsgSender;
	// Set to true after unsuccessfully trying to get the value of an argument
	private boolean _error;

	/**
	 * Creates an instance of the class
	 * @param command Command that contains the argument that will be accessed through this class
	 * @param errorMsgSender Object used to send error messages
	 */
	public CommandArgumentList(SlashCommandInteractionOptionsProvider command, MessageSender errorMsgSender) {
		this.command = command;
		this.errorMsgSender = errorMsgSender;
	}

	/**
	 * Used to check if all the arguments requested were retrieved successfully.
	 * Just checking if they are null is not enough, since that can be a valid return value under certain situarions
	 * (such as optional arguments).
	 * @return True if all the arguments requested were returned successfully, false if at least one caused an error.
	 */
	public boolean success() {
		return !_error;
	}

	/**
	 * Gets an argument of the specified type. If an error happens while trying to get the value of the argument,
	 * the error flag is set.
	 * @param argName Name of the argument
	 * @param getter Function used to get the optional value of the argument
	 * @param required True if the argument is required, false otherwise.
	 * @return Argument value, or null if the argument doesn't have a value or if an error happened
	 * @param <T> Argument type
	 */
	public <T> T get(String argName, ArgumentValueGetter<T> getter, boolean required) {
		SlashCommandInteractionOption argument = getArgument(command, argName, required);
		if (argument != null) {
			// Get the value of the argument
			return getArgumentValue(argument, getter, required);
		} else {
			return null;
		}
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for string values
	 */
	public String getString(String argName, boolean required) {
		return get(argName, SlashCommandInteractionOption::getStringValue, required);
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for Long values
	 */
	public Long getLong(String argName, boolean required) {
		return get(argName, SlashCommandInteractionOption::getLongValue, required);
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for Integer values.
	 * Warning: Values are retrieved as a Long and then cast to an int. If the read Long value is too big to fit on
	 * an integer, an error will be printed.
	 */
	public Integer getInteger(String argName, boolean required) {
		Long valLong = get(argName, SlashCommandInteractionOption::getLongValue, required);
		if (valLong == null) {
			return null;
		} else {
			int valInt = valLong.intValue();
			if (valInt == valLong) {
				return valInt;
			} else {
				errorMsgSender.send("**Error**: The specified value for argument \"" + argName + "\" is too big to " +
					"fit in an integer.");
				_error = true;
				return null;
			}
		}
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for boolean values
	 */
	public Boolean getBoolean(String argName, boolean required) {
		return get(argName, SlashCommandInteractionOption::getBooleanValue, required);
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for User values. Only works with currently
	 * cached users.
	 */
	public User getCachedUser(String argName, boolean required) {
		return get(argName, SlashCommandInteractionOption::getUserValue, required);
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for User values.
	 */
	public CompletableFuture<User> getUser(String argName, boolean required) {
		return get(argName, SlashCommandInteractionOption::requestUserValue, required);
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for Channel values.
	 */
	public Channel getChannel(String argName, boolean required) {
		return get(argName, SlashCommandInteractionOption::getChannelValue, required);
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for Attachment values.
	 */
	public Attachment getAttachment(String argName, boolean required) {
		return get(argName, SlashCommandInteractionOption::getAttachmentValue, required);
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for Role values.
	 */
	public Role getRole(String argName, boolean required) {
		return get(argName, SlashCommandInteractionOption::getRoleValue, required);
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for Mentionable values (roles and users).
	 * Users are only returned if they are currently cached.
	 */
	public Mentionable getCachedMentionable(String argName, boolean required) {
		return get(argName, SlashCommandInteractionOption::getMentionableValue, required);
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for Mentionable values.
	 */
	public CompletableFuture<Mentionable> getMentionable(String argName, boolean required) {
		return get(argName, SlashCommandInteractionOption::requestMentionableValue, required);
	}

	/**
	 * Convenience version of {@link #get(String, ArgumentValueGetter, boolean)} for Double values.
	 */
	public Double getDouble(String argName, boolean required) {
		return get(argName, SlashCommandInteractionOption::getDecimalValue, required);
	}

	/**
	 * Given a command and an argument name, returns the Option object associated to it, or null if the command doesn't
	 * have that argument. If that's the case and the argument is required, an error will be printed to the interaction
	 * specified when this instance was created as well.
	 * @param command Command to get the argument from
	 * @param argumentName Name of the argument
	 * @param required True if the argument is required
	 * @return The argument, or null if it's missing
	 */
	private SlashCommandInteractionOption getArgument(SlashCommandInteractionOptionsProvider command,
	String argumentName, boolean required) {
		AtomicReference<SlashCommandInteractionOption> result = new AtomicReference<>();

		command.getArgumentByName(argumentName).ifPresentOrElse(result::set, () -> {
				if (required) {
					errorMsgSender.send("**Error**: Missing \"" + argumentName + "\" argument");
					_error = true;
				}
				result.set(null);
			}
		);
		return result.get();
	}

	/**
	 * Given some code that can get the value of an argument of a given type, tries to get the value and returns it.
	 * If the value cannot be retrieved for whatever reason or is missing, returns null.
	 * If the argument holds a value but it cannot be retrieved due to an error, prints the error and
	 * sets the error flag.
	 * @param argument Argument object that might contain a value
	 * @param getter Code capable of getting the optional value of the argument
	 * @param required True if the argument is required. If the value cannot be read for whatever reason, it will be
	 *                 considered an error.
	 * @return Argument value, or null if it's empty or an error occurred
	 * @param <T> Type of the argument
	 */
	private <T> T getArgumentValue(SlashCommandInteractionOption argument, ArgumentValueGetter<T> getter,
	boolean required) {
		Optional<T> value = getter.getValue(argument);
		if (value.isEmpty() && (required || argument.getStringRepresentationValue().isPresent())) {
			/*
				Either the argument is required but doesn't have a value or we couldn't get the value, but the argument
				does hold a value in it (in that case, the specified getter was probably incorrect). This is an error.
			 */
			errorMsgSender.send("**Error**: Couldn't get value for argument \"" + argument.getName() + "\".");
			_error = true;
			return null;
		} else {
			return value.orElse(null);
		}
	}

	/**
	 * Interface that represents some code used to get the value of an argument
	 * @param <T> Type of the argument
	 */
	@FunctionalInterface
	public interface ArgumentValueGetter<T> {
		/**
		 * Attempts to retrieve the value of the given argument. If the value is missing or cannot be retrieved due
		 * to an error, returns an empty instance
		 * @param argument Argument whose value will be read
		 * @return Argument value, or empty instance if it doesn't have a value or an error occurs.
		 */
		Optional<T> getValue(SlashCommandInteractionOption argument);
	}
}
