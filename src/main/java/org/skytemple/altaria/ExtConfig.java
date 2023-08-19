package org.skytemple.altaria;

import org.apache.logging.log4j.Level;

import java.util.Optional;

/**
 * Class used to retrieve external configuration values for the bot.
 */
public class ExtConfig {
	private static final String BOT_TOKEN_ENV = "BOT_TOKEN";
	private static final String LOG_LEVEL_ENV = "LOG_LEVEL";

	private static final Level DEFAULT_LOG_LEVEL = Level.INFO;

	/**
	 * @return The Discord token used to authenticate the bot.
	 */
	public static String getBotToken() {
		return Env.getString(BOT_TOKEN_ENV).orElseThrow(() -> new RuntimeException("Bot token must be specified " +
			"on the " + BOT_TOKEN_ENV + " environment variable."));
	}

	public static Level getLogLevel() {
		Optional<String> levelOptStr = Env.getString(LOG_LEVEL_ENV);
		if (levelOptStr.isPresent()) {
			String levelStr = levelOptStr.get();
			try {
				return Level.valueOf(levelStr);
			} catch (IllegalArgumentException e) {
				return DEFAULT_LOG_LEVEL;
			}
		} else {
			return DEFAULT_LOG_LEVEL;
		}
	}
}
