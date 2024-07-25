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

import org.junit.jupiter.api.Test;
import org.skytemple.altaria.definitions.exceptions.GpAmountParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GpAmountParserTest {
	@Test
	void testParseGpAmountSimple1() throws GpAmountParseException {
		assertEquals(1, GpAmountParser.parseGpAmount("1"), 1.0e-6);
	}

	@Test
	void testParseGpAmountSimple2() throws GpAmountParseException {
		assertEquals(1.45, GpAmountParser.parseGpAmount("1.45"), 1.0e-6);
	}

	@Test
	void testParseGpAmountSimple3() throws GpAmountParseException {
		assertEquals(-1.25, GpAmountParser.parseGpAmount("-1.25"), 1.0e-6);
	}

	@Test
	void testParseGpAmountSimple4() throws GpAmountParseException {
		assertEquals(10000, GpAmountParser.parseGpAmount("1e4"), 1.0e-6);
	}

	@Test
	void testParseGpAmountCompound1() throws GpAmountParseException {
		assertEquals(5, GpAmountParser.parseGpAmount("2+3"), 1.0e-6);
	}

	@Test
	void testParseGpAmountCompound2() throws GpAmountParseException {
		assertEquals(5, GpAmountParser.parseGpAmount("2 + 3   "), 1.0e-6);
	}

	@Test
	void testParseGpAmountCompound3() throws GpAmountParseException {
		assertEquals(GpAmountParser.DUNGEON_SPRITE_ANIMATION_GP, GpAmountParser.parseGpAmount("1da"), 1.0e-6);
	}

	@Test
	void testParseGpAmountCompound4() throws GpAmountParseException {
		assertEquals(GpAmountParser.DUNGEON_SPRITE_ANIMATION_GP * 2 + GpAmountParser.PORTRAIT_EMOTION_GP * 6,
			GpAmountParser.parseGpAmount("2da + 6p"), 1.0e-6);
	}

	@Test
	void testParseGpAmountCompound5() throws GpAmountParseException {
		assertEquals(GpAmountParser.DUNGEON_SPRITE_ANIMATION_GP * 2 + GpAmountParser.PORTRAIT_EMOTION_GP * 6 + 45,
			GpAmountParser.parseGpAmount("2da + 6p + 45"), 1.0e-6);
	}

	@Test
	void testParseGpAmountCompound6() throws GpAmountParseException {
		assertEquals(0, GpAmountParser.parseGpAmount("0+0+0+0+0"), 1.0e-6);
	}

	@Test
	void testParseGpAmountInvalid1() {
		assertThrows(GpAmountParseException.class, () -> GpAmountParser.parseGpAmount("a"));
	}

	@Test
	void testParseGpAmountInvalid2() {
		assertThrows(GpAmountParseException.class, () -> GpAmountParser.parseGpAmount("12a"));
	}

	@Test
	void testParseGpAmountInvalid3() {
		assertThrows(GpAmountParseException.class, () -> GpAmountParser.parseGpAmount("12daa"));
	}

	@Test
	void testParseGpAmountInvalid4() {
		assertThrows(GpAmountParseException.class, () -> GpAmountParser.parseGpAmount("12da+"));
	}

	@Test
	void testParseGpAmountInvalid5() {
		assertThrows(GpAmountParseException.class, () -> GpAmountParser.parseGpAmount("+12da"));
	}

	@Test
	void testParseGpAmountInvalid6() {
		assertThrows(GpAmountParseException.class, () -> GpAmountParser.parseGpAmount("+12da+ "));
	}

	@Test
	void testParseGpAmountInvalid7() {
		assertThrows(GpAmountParseException.class, () -> GpAmountParser.parseGpAmount("12da + -4"));
	}

	@Test
	void testParseGpAmountInvalid8() {
		assertThrows(GpAmountParseException.class, () -> GpAmountParser.parseGpAmount("12da + 5pp"));
	}

	@Test
	void testParseGpAmountInvalid9() {
		assertThrows(GpAmountParseException.class, () -> GpAmountParser.parseGpAmount("+"));
	}

	@Test
	void testParseGpAmountInvalid10() {
		assertThrows(GpAmountParseException.class, () -> GpAmountParser.parseGpAmount(""));
	}
}
