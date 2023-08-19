package org.skytemple.altaria;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class Utils {
	/**
	 * Returns a logger
	 * @param c Class whose name will be used as the logger name
	 * @param level Logging level
	 * @return Logger
	 */
	public static Logger getLogger(Class<?> c, Level level) {
		Logger logger = LogManager.getLogger(Main.class);
		Configurator.setLevel(logger, level);
		return logger;
	}

	/**
	 * Returns a logger with the default logging level.
	 * @param c Class whose name will be used as the logger name
	 * @return Logger
	 */
	public static Logger getLogger(Class<?> c) {
		return getLogger(c, ExtConfig.getLogLevel());
	}
}
