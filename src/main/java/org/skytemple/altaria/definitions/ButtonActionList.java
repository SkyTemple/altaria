/*
 * Copyright (c) 2026. Frostbyte and other contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.skytemple.altaria.definitions;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used to store pending actions for an interaction button. Useful when a command shows a user a confirmation
 * message and they have to click a button to actually run the operation.
 * <p>
 * This class assigns a unique custom interaction ID to each stored action, which allows identifying them again when
 * the user confirms the operation by clicking the button.
 * <p>
 * This class is thread safe.
 * @param <T> Type of the object that will be used to represent a pending action
 */
public class ButtonActionList<T> {
	private final String buttonId;
	private final int maxSize;

	/**
	 * Keeps track of the incremental numerical suffix that will be added to button IDs to uniquely identify each
	 * pending action.
	 * Must be read and updated by calling {@link #getNextNonce()}.
	 */
	private int nonce;

	private final List<ActionContainer<T>> actionList;

	/**
	 * Creates a new instance.
	 * @param buttonId Unique string to use to identify the action buttons that this class will handle.
	 * @param maxSize Maximum list size. If exceeded, older entries will start getting deleted.
	 */
	public ButtonActionList(String buttonId, int maxSize) {
		this.buttonId = buttonId;
		this.maxSize = maxSize;

		nonce = 0;
		actionList = new ArrayList<>();
	}

	/**
	 * Stores a pending action to run when the user presses the confirmation button.
	 * @param action Action to store
	 * @return String to use as the custom ID for the interaction button. This ID will uniquely identify the provided
	 * action.
	 */
	public String storeAction(T action) {
		int id = getNextNonce();

		synchronized (this) {
			actionList.add(new ActionContainer<>(id, action));
		}
		purgeOldEntries();

		return buttonId + ":" + id;
	}

	/**
	 * Given the custom ID of a received interaction, returns true if the interaction should be handled using this
	 * instance.
	 * @param customInteractionId Custom ID of the received interaction.
	 * @return True if this object should be used to retrieve the action to perform as a response to the given
	 * interaction, false otherwise.
	 */
	public boolean shouldHandleInteraction(String customInteractionId) {
		return customInteractionId.startsWith(buttonId + ":");
	}

	/**
	 * Returns the pending action entry that corresponds to the given interaction id, or null if there's not a pending
	 * action with the given ID. The action will remain on the list, and can therefore be reran by clicking the
	 * interaction button again.
	 * <p>
	 * This method will throw an exception if the given interaction ID does not correspond to the interactions this
	 * instance was set to handle on creation. Use {@link #shouldHandleInteraction(String)} to ensure that the given ID
	 * corresponds to this instance before calling this method.
	 * @param customInteractionId Custom interaction ID
	 * @return Pending action associated with the given ID, or none if there's no pending action for this ID.
	 * @throws IllegalArgumentException If the given interaction ID does not match the ID handled by this instance.
	 */
	public T getAction(String customInteractionId) {
		ActionContainer<T> actionContainer = getActionContainer(customInteractionId);

		if (actionContainer == null) {
			return null;
		}
		return actionContainer.action;
	}

	/**
	 * Returns the pending action entry that corresponds to the given interaction id, or null if there's not a pending
	 * action with the given ID. The action will be removed from the list, so pressing the button a second time will
	 * have no effect (this method will return null the second time since the ID will no longer be on the list).
	 * <p>
	 * This method will throw an exception if the given interaction ID does not correspond to the interactions this
	 * instance was set to handle on creation. Use {@link #shouldHandleInteraction(String)} to ensure that the given ID
	 * corresponds to this instance before calling this method.
	 * @param customInteractionId Custom interaction ID
	 * @return Pending action associated with the given ID, or none if there's no pending action for this ID.
	 * @throws IllegalArgumentException If the given interaction ID does not match the ID handled by this instance.
	 */
	public T popAction(String customInteractionId) {
		ActionContainer<T> actionContainer = getActionContainer(customInteractionId);

		if (actionContainer == null) {
			return null;
		}

		synchronized (this) {
			actionList.remove(actionContainer);
		}
		return actionContainer.action;
	}

	private synchronized int getNextNonce() {
		nonce++;
		return nonce;
	}

	private ActionContainer<T> getActionContainer(String customInteractionId) {
		if (!shouldHandleInteraction(customInteractionId)) {
			throw new IllegalArgumentException("Interaction ID '" + customInteractionId + "' does not match ID " +
				"prefix for this instace (" + buttonId + ":)");
		}

		int numericId = Integer.parseInt(customInteractionId.split(":")[1]);

		synchronized (this) {
			// The list is sorted, so this could be optimized using binary search, but these lists are small so it's not
			// worth it.
			for (ActionContainer<T> actionContainer : actionList) {
				if (actionContainer.id == numericId) {
					return actionContainer;
				}
				if (actionContainer.id > numericId) {
					break;
				}
			}
		}

		return null;
	}

	/**
	 * Checks if the action list exceeds maximum size, and purges oldest entries if so.
	 */
	private synchronized void purgeOldEntries() {
		while (actionList.size() > maxSize) {
			actionList.remove(0);
		}
	}

	/**
	 * Used to store an action and its numerical ID.
	 * @param <T> Action type
	 */
	private record ActionContainer<T>(int id, T action) {}
}
