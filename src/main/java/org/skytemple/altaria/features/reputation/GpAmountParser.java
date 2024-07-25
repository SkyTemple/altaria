/*
 * Copyright (c) 2024. Frostbyte and other contributors.
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

package org.skytemple.altaria.features.reputation;

import org.skytemple.altaria.definitions.exceptions.GpAmountParseException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class used to parse strings that represent an amount of GP.
 * The strings can be specified using simple syntax (single decimal value) or compound syntax (sum of multiple terms)
 */
public class GpAmountParser {
	private static final Pattern TERM_REGEX = Pattern.compile("^(?<amount>\\d+)(?<suffix>da|sa|oa|p|ap|sr)?$");

	public static final double DUNGEON_SPRITE_ANIMATION_GP = 12/10.0;
	public static final double STARTER_SPRITE_ANIMATION_GP = 12/22.0;
	public static final double OTHER_SPRITE_ANIMATION_GP = 1.0;
	public static final double PORTRAIT_EMOTION_GP = 6/20.0;
	public static final double ASYM_PORTRAIT_EMOTION_GP = 2/20.0;
	public static final double SHINY_OR_RECOLOR_SPRITE_GP = 1.0;

	/**
	 * Attempts to parse a string as a GP amount
	 * @param amountStr String to parse. Should be an amount specified using either simple syntax or compound syntax.
	 * @return Equivalent amount, as a double
	 * @throws GpAmountParseException If the amount cannot be parsed
	 */
	public static double parseGpAmount(String amountStr) throws GpAmountParseException {
		// Try to parse using simple syntax (simply parsing it as a double)
		try {
			return Double.parseDouble(amountStr);
		} catch (NumberFormatException e) {
			// Not a number, continue
		}

		// Try to parse using compound syntax

		double amount = 0;
		// Whitespace is ignored
		amountStr = amountStr.replace(" ", "");

		// Since String.split excludes trailing empty groups, we have to check this manually
		if (amountStr.endsWith("+")) {
			throw new GpAmountParseException();
		}

		String[] terms = amountStr.split("\\+");
		for (String term : terms) {
			amount += parseTerm(term);
		}
		return amount;
	}

	/**
	 * Attempts to parse a single term of a GP amount specified using compound syntax
	 * @param term Term to parse
	 * @return Value of the term, as a double
	 * @throws GpAmountParseException If the term does not have valid syntax
	 */
	private static double parseTerm(String term) throws GpAmountParseException {
		Matcher matcher = TERM_REGEX.matcher(term);

		if (!matcher.matches()) {
			throw new GpAmountParseException("Invalid term for compound GP amount: " + term);
		}

		String amountStr = matcher.group("amount");
		String suffix = matcher.group("suffix");
		int amount;
		try {
			amount = Integer.parseInt(amountStr);
		} catch (NumberFormatException e) {
			throw new GpAmountParseException(e);
		}

		if (suffix == null) {
			return amount;
		} else if (suffix.equals("da")) {
			return amount * DUNGEON_SPRITE_ANIMATION_GP;
		} else if (suffix.equals("sa")) {
			return amount * STARTER_SPRITE_ANIMATION_GP;
		} else if (suffix.equals("oa")) {
			return amount * OTHER_SPRITE_ANIMATION_GP;
		} else if (suffix.equals("p")) {
			return amount * PORTRAIT_EMOTION_GP;
		} else if (suffix.equals("ap")) {
			return amount * ASYM_PORTRAIT_EMOTION_GP;
		} else if (suffix.equals("sr")) {
			return amount * SHINY_OR_RECOLOR_SPRITE_GP;
		} else {
			throw new GpAmountParseException("Invalid GP amount suffix: " + suffix);
		}
	}
}
