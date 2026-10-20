package space.nebula.nexus.utils;

import space.nebula.nexus.enums.LifeListStatus;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * Pure status rules for a life-list item. Completing a wish never creates a
 * memory, and reversing completion never deletes one.
 */
public final class LifeListStatusPolicy {

	private static final Set<LifeListStatus> ALL = EnumSet.allOf(LifeListStatus.class);

	private LifeListStatusPolicy() {
	}

	public static boolean canTransition(LifeListStatus from, LifeListStatus to) {
		if (from == null || to == null) {
			return false;
		}
		if (from == to) {
			return true;
		}
		return ALL.contains(from) && ALL.contains(to);
	}

	/**
	 * Completing writes the supplied date, or today when none is given. Leaving
	 * completed clears the current completion date. Other transitions leave it
	 * untouched.
	 */
	public static LocalDate nextCompletedAt(LifeListStatus to, LocalDate requested, LocalDate today) {
		if (to == LifeListStatus.COMPLETED) {
			return requested != null ? requested : today;
		}
		if (to == LifeListStatus.PLANNED || to == LifeListStatus.IN_PROGRESS) {
			return null;
		}
		return null;
	}

	public static boolean clearsCompletedAt(LifeListStatus to) {
		return to != LifeListStatus.COMPLETED;
	}
}
