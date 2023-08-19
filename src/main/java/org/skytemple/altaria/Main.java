package org.skytemple.altaria;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;

import org.apache.logging.log4j.Logger;
import org.skytemple.altaria.db.Database;
import org.skytemple.altaria.db.ReputationDB;

public class Main {

	public static void main(String[] args) {
		Logger logger = Utils.getLogger(Main.class);

		String token = ExtConfig.getBotToken();
		DiscordApi api = new DiscordApiBuilder().setToken(token).addIntents(Intent.MESSAGE_CONTENT).login().join();
		Database db = new Database(ExtConfig.getDbHost(), ExtConfig.getDbPort(), ExtConfig.getDbUsername(),
			ExtConfig.getDbPassword(), ExtConfig.getDbDatabase());

		logger.info("Bot started. Invite URL: " + api.createBotInvite());

		ReputationDB reputation = new ReputationDB(db);

		// Add a listener which answers with "Pong!" if someone writes "!ping"
		api.addMessageCreateListener(event -> {
			if (event.getMessageContent().equalsIgnoreCase("!ping")) {
				event.getChannel().sendMessage("Pong!");
			}
		});
	}
}