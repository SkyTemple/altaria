package org.skytemple.altaria;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;

import org.apache.logging.log4j.Logger;
import org.skytemple.altaria.definitions.db.Database;
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
	}
}