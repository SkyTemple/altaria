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

package org.skytemple.altaria.features.fun.fun2026;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class used to keep track of the recolor cost of each individual role
 */
public class RoleRecolorCosts {
	private int halfLife;

	private final Map<Long, RoleRecolorCost> costs;

	/**
	 * Creates a new instance
	 * @param costHalfLifeMinutes Half-life value to use for the cost exponential decay, in minutes
	 */
	public RoleRecolorCosts(int costHalfLifeMinutes) {
		halfLife = costHalfLifeMinutes;

		costs = new TreeMap<>();
	}

	/**
	 * Sets the half-life value to use for the cost exponential decay, in minutes
	 * @param halfLife New half-life value
	 */
	public void setHalfLife(int halfLife) {
		this.halfLife = halfLife;
	}

	public synchronized int getCost(long roleId) {
		RoleRecolorCost currentCost = costs.get(roleId);
		if (currentCost == null) {
			return 0;
		} else {
			long elapsedMinutes = ChronoUnit.MINUTES.between(currentCost.lastRecolorTimestamp, ZonedDateTime.now());
			// Exponential decay
			return Math.toIntExact(Math.round(currentCost.cost * Math.pow(0.5, ((double) elapsedMinutes / halfLife))));
		}
	}

	public synchronized void setCost(long roleId, int cost) {
		RoleRecolorCost currentCost = costs.get(roleId);
		if (currentCost == null) {
			costs.put(roleId, new RoleRecolorCost(cost, ZonedDateTime.now()));
		} else {
			currentCost.cost = cost;
			currentCost.lastRecolorTimestamp = ZonedDateTime.now();
		}
	}

	private static final class RoleRecolorCost {
		public int cost;
		public ZonedDateTime lastRecolorTimestamp;

		private RoleRecolorCost(int cost, ZonedDateTime lastRecolorTimestamp) {
			this.cost = cost;
			this.lastRecolorTimestamp = lastRecolorTimestamp;
		}
	}
}
