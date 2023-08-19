package org.skytemple.altaria;

import org.apache.logging.log4j.Level;
import org.skytemple.altaria.exceptions.FatalErrorException;

import java.util.Optional;

/**
 * Class used to retrieve external configuration values for the bot.
 */
public class ExtConfig {
	private static final String BOT_TOKEN_ENV = "BOT_TOKEN";
	private static final String LOG_LEVEL_ENV = "LOG_LEVEL";
	private static final String DB_HOST_ENV = "DB_HOST";
	private static final String DB_PORT_ENV = "DB_PORT";
	private static final String DB_USER_ENV = "DB_USER";
	private static final String DB_PASSWORD_ENV = "DB_PASSWORD";
	private static final String DB_DATABASE_ENV = "DB_DATABASE";

	private static final Level DEFAULT_LOG_LEVEL = Level.INFO;

	/**
	 * @return The Discord token used to authenticate the bot.
	 */
	public static String getBotToken() {
		return Env.getString(BOT_TOKEN_ENV).orElseThrow(() -> new FatalErrorException("Bot token must be specified " +
			"on the " + BOT_TOKEN_ENV + " environment variable."));
	}

	/**
	 * Returns the log level to use when logging messages. If no log level has been specified, returns
	 * {@link #DEFAULT_LOG_LEVEL}.
	 * @return Set log level for message logging
	 */
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

	/**
	 * @return The database host used for connection
	 */
	public static String getDbHost() {
		return Env.getString(DB_HOST_ENV).orElseThrow(() -> new FatalErrorException("DB host must be specified " +
			"on the " + DB_HOST_ENV + " environment variable."));
	}

	/**
	 * @return The database port used for connection
	 */
	public static String getDbPort() {
		return Env.getString(DB_PORT_ENV).orElseThrow(() -> new FatalErrorException("DB port must be specified " +
			"on the " + DB_PORT_ENV + " environment variable."));
	}

	/**
	 * @return The database username used for connection
	 */
	public static String getDbUsername() {
		return Env.getString(DB_USER_ENV).orElseThrow(() -> new FatalErrorException("DB username must be specified " +
			"on the " + DB_USER_ENV + " environment variable."));
	}

	/**
	 * @return The database password used for connection
	 */
	public static String getDbPassword() {
		return Env.getString(DB_PASSWORD_ENV).orElseThrow(() -> new FatalErrorException("DB password must be specified " +
			"on the " + DB_PASSWORD_ENV + " environment variable."));
	}

	/**
	 * @return The database MySQL database used for connection
	 */
	public static String getDbDatabase() {
		return Env.getString(DB_DATABASE_ENV).orElseThrow(() -> new FatalErrorException("DB database must be specified " +
			"on the " + DB_DATABASE_ENV + " environment variable."));
	}
}
