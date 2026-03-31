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

package org.skytemple.altaria.definitions.exceptions;

/**
 * Thrown when invalid data is read from an input file.
 */
public class FileDataParseException extends Exception {
	public FileDataParseException(String message) {
		super(message);
	}

	public FileDataParseException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileDataParseException(Throwable cause) {
		super(cause);
	}
}
