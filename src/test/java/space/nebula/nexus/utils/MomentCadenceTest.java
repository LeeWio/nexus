package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.payload.response.MomentCadenceResponse;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MomentCadenceTest {

	private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 10, 13, 15, 30, 0, 0, MomentCadence.ZONE);

	@Test
	void placesStoredUtcTimestampsIntoTheAuthorTimeZone() {
		MomentCadenceResponse cadence = MomentCadence.of(Arrays.asList(LocalDateTime.of(2026, 10, 12, 16, 30), // 2026-10-13
																												// 00:30
																												// in
																												// Shanghai
				LocalDateTime.of(2026, 10, 13, 7, 0), // 15:00, same instant twice
				LocalDateTime.of(2026, 10, 13, 7, 45), LocalDateTime.of(2026, 1, 1, 0, 0), // January in Shanghai
				LocalDateTime.of(2025, 12, 31, 15, 59), // still 2025 in Shanghai
				LocalDateTime.of(2026, 12, 31, 16, 0), // already 2027 in Shanghai
				null), NOW);

		assertEquals("Asia/Shanghai", cadence.zone());
		assertEquals(NOW.toLocalDate().atStartOfDay(MomentCadence.ZONE).toOffsetDateTime(), cadence.day().start());
		assertEquals(List.of(0, 15), cadence.day().active());
		assertEquals(31, cadence.month().days());
		assertEquals(4, cadence.month().offset());
		assertEquals(List.of(12), cadence.month().active());
		assertEquals(2026, cadence.year().year());
		assertEquals(List.of(0, 37), cadence.year().active());
	}

	@Test
	void usesTheAuthorYearBoundariesForTheStoredQueryRange() {
		assertEquals(LocalDateTime.of(2025, 12, 31, 16, 0), MomentCadence.storedRangeStart(NOW));
		assertEquals(LocalDateTime.of(2026, 12, 31, 16, 0), MomentCadence.storedRangeEnd(NOW));
	}

	@Test
	void keepsALeapDayInsideFebruary() {
		ZonedDateTime leapNow = ZonedDateTime.of(2028, 2, 29, 1, 0, 0, 0, MomentCadence.ZONE);
		LocalDateTime leapDay = leapNow.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

		MomentCadenceResponse cadence = MomentCadence.of(List.of(leapDay), leapNow);

		assertEquals(29, cadence.month().days());
		assertEquals(2, cadence.month().offset());
		assertEquals(List.of(28), cadence.month().active());
		assertEquals(List.of(1), cadence.day().active());
		assertEquals(List.of(7), cadence.year().active());
	}

	@Test
	void foldsAFifthWeekIntoTheFourthBucket() {
		assertEquals(0, MomentCadence.weekBucket(java.time.LocalDate.of(2026, 1, 1)));
		assertEquals(3, MomentCadence.weekBucket(java.time.LocalDate.of(2026, 1, 22)));
		assertEquals(3, MomentCadence.weekBucket(java.time.LocalDate.of(2026, 1, 31)));
		assertEquals(47, MomentCadence.weekBucket(java.time.LocalDate.of(2026, 12, 31)));
	}

	@Test
	void returnsNoActiveBucketsWhenNothingWasPublished() {
		MomentCadenceResponse cadence = MomentCadence.of(List.of(), NOW);

		assertEquals(List.of(), cadence.day().active());
		assertEquals(List.of(), cadence.month().active());
		assertEquals(List.of(), cadence.year().active());
	}
}
