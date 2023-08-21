package org.skytemple.altaria.definitions.message_sender;

/**
 * Represents an object capable of sending a Discord message
 */
public interface MessageSender {
	/**
	 * Sends the specified message
	 * @param message Message to send
	 */
	void send(String message);
}
