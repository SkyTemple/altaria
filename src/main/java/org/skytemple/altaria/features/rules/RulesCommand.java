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

package org.skytemple.altaria.features.rules;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.skytemple.altaria.definitions.Command;
import org.skytemple.altaria.definitions.senders.MessageSender;

import java.awt.*;
import java.util.Map;

public class RulesCommand implements Command {
	protected Map<String, String> rules;
	protected String ruleNumber;
	protected MessageSender resultSender;
	protected MessageSender errorSender;

	/**
	 * Prints one of the server rules
	 * @param rules Map containing the server rules
	 * @param ruleNumber Number of the rule to print (can be any string)
	 * @param resultSender Used to send result messages to the user
	 * @param errorSender Used to send error messages to the user
	 */
	public RulesCommand(Map<String, String> rules, String ruleNumber, MessageSender resultSender,
	MessageSender errorSender) {
		this.rules = rules;
		this.ruleNumber = ruleNumber;
		this.resultSender = resultSender;
		this.errorSender = errorSender;
	}

	@Override
	public void run() {
		String rule = rules.get(ruleNumber);
		if (rule != null) {
			EmbedBuilder embed = new EmbedBuilder().setDescription(rule).setColor(Color.BLUE);
			resultSender.addEmbed(embed).send();
		} else {
			errorSender.send("Error: Rule " + ruleNumber + " not found.");
		}
	}
}
