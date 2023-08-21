package org.skytemple.altaria.definitions.singletons;

import org.javacord.api.DiscordApi;
import org.skytemple.altaria.definitions.exceptions.IllegalOperationException;

/**
 * Class used to store a single {@link DiscordApi} instance and retrieve it from anywhere.
 */
public class ApiGetter {
	private static DiscordApi api;

	protected ApiGetter() {}

	/**
	 * Initializes the object. Must be called before any calls to the {@link #get()} method happen.
	 * Calling this method multiple times has no effect.
	 * @param api Discord API object, already initialized.
	 */
	public static void init(DiscordApi api) {
		if (ApiGetter.api == null) {
			ApiGetter.api = api;
		}
	}

	/**
	 * Gets the instance of this object. {@link #init} must be called at least once first.
	 * @return The object's single instance
	 */
	public static DiscordApi get() {
		if (api == null) {
			throw new IllegalOperationException("ApiGetter must be initialized before get() can be called");
		} else {
			//noinspection StaticVariableUsedBeforeInitialization
			return api;
		}
	}
}
