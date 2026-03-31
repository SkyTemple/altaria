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
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.List;

import static org.skytemple.altaria.definitions.Constants.MAX_ROLE_MATCHES;

/**
 * Class used to retrieve a role by providing a search query. Supports multiple and partial matches.
 */
public class RoleMatcher {
	private final Server server;
	private final int maxMatches;

	/**
	 * Creates a new instance
	 * @param server Server to search roles in
	 * @param maxMatches Max number of roles to match for a given query
	 */
	public RoleMatcher(Server server, int maxMatches) {
		this.server = server;
		this.maxMatches = maxMatches;
	}

	/**
	 * Creates a new instance, using the global default max matches value
	 * @param server Server to search roles in
	 */
	public RoleMatcher(Server server) {
		this.server = server;
		maxMatches = MAX_ROLE_MATCHES;
	}

	/**
	 * Searches for roles that match the given search query and returns a result
	 * @param query Search query
	 * @return RoleMatch object representing the result of the search
	 */
	public RoleMatch findRole(String query) {
		if (query.isEmpty()) {
			// Match nothing
			return new RoleMatch(query, new ArrayList<>(), false);
		}

		ArrayList<Role> matchList = new ArrayList<>();
		int matches = 0;
		boolean trimmed = false;

		for (Role role : server.getRoles()) {
			String roleName = role.getName();

			if (roleName.equalsIgnoreCase(query)) {
				// Exact match, return this role only
				return new RoleMatch(query, List.of(role), false);
			} else if (roleName.toLowerCase().contains(query.toLowerCase())) {
				matchList.add(role);
				matches++;
			}

			if (matches == maxMatches) {
				// Too many matches, trim the list here
				trimmed = true;
				break;
			}
		}

		return new RoleMatch(query, matchList, trimmed);
	}
}
