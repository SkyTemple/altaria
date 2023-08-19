package org.skytemple.altaria;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class Utils {
	public static Logger getLogger(Class<?> c, Level level) {
		Logger logger = LogManager.getLogger(Main.class);
		Configurator.setLevel(logger, level);
		return logger;
	}
}
