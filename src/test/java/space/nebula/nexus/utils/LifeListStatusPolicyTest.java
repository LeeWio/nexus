package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.enums.LifeListStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifeListStatusPolicyTest {

	private final LocalDate today = LocalDate.of(2026, 10, 20);

	@Test
	void allowsEveryDocumentedTransition() {
		assertTrue(LifeListStatusPolicy.canTransition(LifeListStatus.PLANNED, LifeListStatus.IN_PROGRESS));
		assertTrue(LifeListStatusPolicy.canTransition(LifeListStatus.PLANNED, LifeListStatus.COMPLETED));
		assertTrue(LifeListStatusPolicy.canTransition(LifeListStatus.IN_PROGRESS, LifeListStatus.COMPLETED));
		assertTrue(LifeListStatusPolicy.canTransition(LifeListStatus.COMPLETED, LifeListStatus.PLANNED));
		assertTrue(LifeListStatusPolicy.canTransition(LifeListStatus.COMPLETED, LifeListStatus.IN_PROGRESS));
		assertTrue(LifeListStatusPolicy.canTransition(LifeListStatus.PLANNED, LifeListStatus.ABANDONED));
		assertTrue(LifeListStatusPolicy.canTransition(LifeListStatus.ABANDONED, LifeListStatus.PLANNED));
		assertTrue(LifeListStatusPolicy.canTransition(LifeListStatus.ABANDONED, LifeListStatus.COMPLETED));
	}

	@Test
	void rejectsMissingStatus() {
		assertFalse(LifeListStatusPolicy.canTransition(null, LifeListStatus.COMPLETED));
		assertFalse(LifeListStatusPolicy.canTransition(LifeListStatus.PLANNED, null));
	}

	@Test
	void completionDateDefaultsToTodayAndClearsWhenReopened() {
		assertEquals(today,
				LifeListStatusPolicy.nextCompletedAt(LifeListStatus.COMPLETED, null, today));
		assertEquals(LocalDate.of(2024, 5, 1),
				LifeListStatusPolicy.nextCompletedAt(LifeListStatus.COMPLETED, LocalDate.of(2024, 5, 1), today));
		assertNull(LifeListStatusPolicy.nextCompletedAt(LifeListStatus.PLANNED, today, today));
		assertNull(LifeListStatusPolicy.nextCompletedAt(LifeListStatus.IN_PROGRESS, today, today));
		assertNull(LifeListStatusPolicy.nextCompletedAt(LifeListStatus.ABANDONED, today, today));
		assertTrue(LifeListStatusPolicy.clearsCompletedAt(LifeListStatus.PLANNED));
		assertFalse(LifeListStatusPolicy.clearsCompletedAt(LifeListStatus.COMPLETED));
	}
}
