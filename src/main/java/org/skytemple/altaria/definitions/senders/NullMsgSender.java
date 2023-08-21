package org.skytemple.altaria.definitions.senders;

/**
 * Discards incoming messages without sending them anywhere.
 */
public class NullMsgSender implements MessageSender {

	@Override
	public void send(String message) {

	}
}
