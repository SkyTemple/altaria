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

package org.skytemple.altaria.definitions.exceptions;

/**
 * Thrown when an asynchronous operation fails and no fallback operation can be performed.
 */
public class AsyncOperationException extends Exception {
	public AsyncOperationException() {

	}

	public AsyncOperationException(String message) {
		super(message);
	}

	public AsyncOperationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AsyncOperationException(Throwable cause) {
		super(cause);
	}
}
