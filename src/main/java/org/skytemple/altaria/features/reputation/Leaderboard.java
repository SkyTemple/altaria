package org.skytemple.altaria.features.reputation;

import org.skytemple.altaria.definitions.db.ReputationDB;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the full GP leaderboard
 */
public class Leaderboard {
	// Number of entries to display on each leaderboard page
	private static final int ENTRIES_PER_PAGE = 25;

	private final List<NamedPointsEntry> data;

	/**
	 * Creates a leaderboard using from a list of point entries
	 * @param pointEntries List of entries used to create the leaderboard
	 */
	public Leaderboard(List<ReputationDB.PointsEntry> pointEntries) {
		data = new ArrayList<>();
		for (ReputationDB.PointsEntry entry : pointEntries) {
			data.add(new NamedPointsEntry("<@" + entry.userId() + ">", entry.points()));
		}
	}

	/**
	 * Returns the number of pages needed to display all the leaderboard entries
	 * @return Number of pages on the leaderboard
	 */
	public int getNumPages() {
		return (int) Math.ceil(data.size() / (float) ENTRIES_PER_PAGE);
	}

	/**
	 * Given a negative page number (used to refer to pages starting from the end), returns the corresponding
	 * positive page number.
	 * @param pageNumber Negative page to convert. Must be < 0.
	 * @return Equivalent positive page number (0-indexed)
	 */
	public int convertNegativePage(int pageNumber) {
		if (pageNumber < 0) {
			return getNumPages() + pageNumber;
		} else {
			throw new IllegalArgumentException("Page number to convert must be negative");
		}
	}

	/**
	 * Gets part of the leaderboard and formats it as a string.
	 * The users will be listed as mentions.
	 * If the requested page is out of bounds, throws {@link IllegalArgumentException}.
	 * @param pageNumber Page number to get (0-indexed). Can be negative to get a page from the bottom of the
	 *                   leaderboard.
	 * @return String that lists all the entries in the specified page
	 */
	public String getPage(int pageNumber) {
		if (pageNumber < 0) {
			pageNumber = convertNegativePage(pageNumber);
		}
		if (pageNumber < getNumPages() && pageNumber >= 0) {
			List<NamedPointsEntry> page = data.subList(pageNumber * ENTRIES_PER_PAGE,
				Math.min(((pageNumber + 1) * ENTRIES_PER_PAGE), data.size()));
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (NamedPointsEntry element : page) {
				if (first) {
					first = false;
				} else {
					sb.append("\n");
				}
				sb.append(element.username).append(": ").append(element.points);
			}
			return sb.toString();
		} else {
			throw new IllegalArgumentException("Page number out of bounds");
		}
	}

	/**
	 * Used to store a leaderboard entry in a readable format
	 * @param username Username or mention
	 * @param points Amount of points
	 */
	private record NamedPointsEntry (String username, int points) {}
}
