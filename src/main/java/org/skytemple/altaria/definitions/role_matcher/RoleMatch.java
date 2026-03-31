/*
 * Copyright (c) 2026. Frostbyte and other contributors.
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

package org.skytemple.altaria.definitions.role_matcher;

import org.javacord.api.entity.permission.Role;

import java.util.List;

/**
 * Represents the result of a query against a {@link RoleMatcher}.
 */
public class RoleMatch {
	private final String query;
	private final List<Role> result;
	private final boolean trimmed;

	/**
	 * Creates a new instance
	 * @param query Query used to search for roles
	 * @param result List of matched roles
	 * @param trimmed True if the result list might have been trimmed due to being too large
	 */
	public RoleMatch(String query, List<Role> result, boolean trimmed) {
		this.query = query;
		this.result = result;
		this.trimmed = trimmed;
	}

	/**
	 * @return Number of roles that matched the given query
	 */
	public int getNumMatches() {
		return result.size();
	}

	/**
	 * @return First role that matched the given query, or null if none did.
	 */
	public Role getFirstMatch() {
		if (result.isEmpty()) {
			return null;
		} else {
			return result.get(0);
		}
	}

	/**
	 * @return List with all the roles that matched the given query
	 */
	public List<Role> getAllMatches() {
		return result;
	}

	/**
	 * @return User-friendly message describing the result of the match.
	 */
	public String getDefaultMessage() {
		int numMatches = getNumMatches();

		if (numMatches == 0) {
			return "No roles matching '" + query + "' found.";
		} else if (numMatches == 1) {
			return "'" + query + "' matched a single role: " + getFirstMatch().getName();
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(result.size());
			if (trimmed) {
				sb.append("+");
			}
			sb.append(" roles found matching '").append(query).append("':\n");

			for (Role match : result) {
				sb.append("- ").append(match.getName()).append("\n");
			}

			if (trimmed) {
				sb.append("- ...");
			}

			return sb.toString();
		}
	}
}
