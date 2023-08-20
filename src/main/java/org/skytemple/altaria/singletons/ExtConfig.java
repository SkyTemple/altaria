package org.skytemple.altaria.singletons;

import org.apache.logging.log4j.Level;
import org.skytemple.altaria.utils.Env;
import org.skytemple.altaria.exceptions.FatalErrorException;

import java.util.Optional;

/**
 * Class used to retrieve external configuration values for the bot.
 * Values will be cached once read, since they are not expected to change until the bot gets restarted.
 */
public class ExtConfig {
	private static final String ENV_BOT_TOKEN = "BOT_TOKEN";
	private static final String ENV_LOG_LEVEL = "LOG_LEVEL";
	private static final String ENV_GUILD_ID = "GUILD_ID";
	private static final String ENV_ERROR_CHANNEL_ID = "ERROR_CHANNEL_ID";
	private static final String ENV_DB_HOST = "DB_HOST";
	private static final String ENV_DB_PORT = "DB_PORT";
	private static final String ENV_DB_USER = "DB_USER";
	private static final String ENV_DB_PASSWORD = "DB_PASSWORD";
	private static final String ENV_DB_DATABASE = "DB_DATABASE";

	private static final Level DEFAULT_LOG_LEVEL = Level.INFO;

	private static ExtConfig instance;

	private String botToken;
	private Level logLevel;
	private Long guildId;
	private Long errorChannelId; // -1 if no channel has been specified
	private String dbHost;
	private String dbPort;
	private String dbUser;
	private String dbPassword;
	private String dbDatabase;

	protected ExtConfig() {
		botToken = null;
		logLevel = null;
		guildId = null;
		errorChannelId = null;
		dbHost = null;
		dbUser = null;
		dbPassword = null;
		dbDatabase = null;
	}

	public static ExtConfig get() {
		if (instance == null) {
			instance = new ExtConfig();
		}
		//noinspection StaticVariableUsedBeforeInitialization
		return instance;
	}

	/**
	 * @return The Discord token used to authenticate the bot.
	 */
	public String getBotToken() {
		if (botToken == null) {
			botToken = Env.getString(ENV_BOT_TOKEN).orElseThrow(() -> new FatalErrorException("Bot token must be " +
				"specified on the " + ENV_BOT_TOKEN + " environment variable."));
		}
		return botToken;
	}

	/**
	 * Returns the log level to use when logging messages. If no log level has been specified, returns
	 * {@link #DEFAULT_LOG_LEVEL}.
	 * @return Set log level for message logging
	 */
	public Level getLogLevel() {
		if (logLevel == null) {
			Optional<String> levelOptStr = Env.getString(ENV_LOG_LEVEL);
			if (levelOptStr.isPresent()) {
				String levelStr = levelOptStr.get();
				try {
					logLevel = Level.valueOf(levelStr);
				} catch (IllegalArgumentException e) {
					logLevel = DEFAULT_LOG_LEVEL;
				}
			} else {
				logLevel = DEFAULT_LOG_LEVEL;
			}
		}
		return logLevel;
	}

	/**
	 * @return The ID of the Discord guild where the bot is going to be deployed. Used to register commands.
	 */
	public long getGuildId() {
		if (guildId == null) {
			guildId = Env.getLong(ENV_GUILD_ID).orElseThrow(() -> new FatalErrorException("Guild ID must be specified " +
				"on the " + ENV_GUILD_ID + " environment variable."));
		}
		return guildId;
	}

	/**
	 * Returns the ID of the channel where full errors should be logged, if it was specified.
	 * @return Channel where the errors should be logged. Empty if an error log channel has not been specified.
	 */
	public Optional<Long> getErrorChannelId() {
		if (errorChannelId == null) {
			errorChannelId = Env.getLong(ENV_ERROR_CHANNEL_ID).orElse(-1L);
		}
		if (errorChannelId == -1) {
			return Optional.empty();
		} else {
			return Optional.of(errorChannelId);
		}
	}

	/**
	 * @return The database host used for connection
	 */
	public String getDbHost() {
		if (dbHost == null) {
			dbHost = Env.getString(ENV_DB_HOST).orElseThrow(() -> new FatalErrorException("DB host must be specified " +
				"on the " + ENV_DB_HOST + " environment variable."));
		}
		return dbHost;
	}

	/**
	 * @return The database port used for connection
	 */
	public String getDbPort() {
		if (dbPort == null) {
			dbPort = Env.getString(ENV_DB_PORT).orElseThrow(() -> new FatalErrorException("DB port must be specified " +
				"on the " + ENV_DB_PORT + " environment variable."));
		}
		return dbPort;
	}

	/**
	 * @return The database username used for connection
	 */
	public String getDbUsername() {
		if (dbUser == null) {
			dbUser = Env.getString(ENV_DB_USER).orElseThrow(() -> new FatalErrorException("DB username must be " +
				"specified on the " + ENV_DB_USER + " environment variable."));
		}
		return dbUser;
	}

	/**
	 * @return The database password used for connection
	 */
	public String getDbPassword() {
		if (dbPassword == null) {
			dbPassword = Env.getString(ENV_DB_PASSWORD).orElseThrow(() -> new FatalErrorException("DB password must be " +
				"specified on the " + ENV_DB_PASSWORD + " environment variable."));
		}
		return dbPassword;
	}

	/**
	 * @return The database MySQL database used for connection
	 */
	public String getDbDatabase() {
		if (dbDatabase == null) {
			dbDatabase = Env.getString(ENV_DB_DATABASE).orElseThrow(() -> new FatalErrorException("DB database must be " +
				"specified on the " + ENV_DB_DATABASE + " environment variable."));
		}
		return dbDatabase;
	}
}
