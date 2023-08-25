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

package org.skytemple.altaria;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;

import org.apache.logging.log4j.Logger;
import org.skytemple.altaria.definitions.db.Database;
import org.skytemple.altaria.features.mod_actions.ModActions;
import org.skytemple.altaria.features.reputation.Reputation;
import org.skytemple.altaria.definitions.singletons.ApiGetter;
import org.skytemple.altaria.definitions.singletons.ExtConfig;
import org.skytemple.altaria.utils.Utils;

public class Main {

	public static void main(String[] args) {
		Logger logger = Utils.getLogger(Main.class);
		ExtConfig extConfig = ExtConfig.get();

		String token = extConfig.getBotToken();
		DiscordApi api = new DiscordApiBuilder().setToken(token).addIntents(Intent.MESSAGE_CONTENT).login().join();
		ApiGetter.init(api);
		Database db = new Database(extConfig.getDbHost(), extConfig.getDbPort(), extConfig.getDbUsername(),
			extConfig.getDbPassword(), extConfig.getDbDatabase());

		logger.info("Bot started. Invite URL: " + api.createBotInvite());

		Reputation reputation = new Reputation(db);
		ModActions modActions = new ModActions();
	}
}