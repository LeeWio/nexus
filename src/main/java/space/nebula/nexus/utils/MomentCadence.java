package space.nebula.nexus.utils;

import space.nebula.nexus.payload.response.MomentCadenceResponse;
import space.nebula.nexus.payload.response.MomentCadenceResponse.DayCadence;
import space.nebula.nexus.payload.response.MomentCadenceResponse.MonthCadence;
import space.nebula.nexus.payload.response.MomentCadenceResponse.YearCadence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TreeSet;

/**
 * Places public moment publication times into the current day, month, and year.
 *
 * <p>
 * Stored timestamps are treated as UTC wall time because the database
 * connection uses {@code serverTimezone=UTC}. Buckets use a fixed author time
 * zone so the same publication does not move when a visitor's clock changes.
 */
public final class MomentCadence {

	/** Author time zone used for every public cadence. */
	public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

	private static final ZoneId STORED_ZONE = ZoneId.of("UTC");

	private MomentCadence() {
	}

	/**
	 * Builds the cadence visible at {@code now} from publication timestamps.
	 * 
	 * @param publishedAt
	 *            publication timestamps stored as UTC wall time; entries outside
	 *            the current year are ignored
	 * @param now
	 *            instant that selects the current day, month, and year
	 * @return active hour, day, and month buckets in the author time zone
	 */
	public static MomentCadenceResponse of(List<LocalDateTime> publishedAt, ZonedDateTime now) {
		ZonedDateTime zonedNow = now.withZoneSameInstant(ZONE);
		LocalDate today = zonedNow.toLocalDate();
		YearMonth month = YearMonth.from(today);
		ZonedDateTime yearStart = today.withDayOfYear(1).atStartOfDay(ZONE);
		ZonedDateTime nextYear = yearStart.plusYears(1);
		ZonedDateTime dayStart = today.atStartOfDay(ZONE);
		ZonedDateTime monthStart = month.atDay(1).atStartOfDay(ZONE);

		TreeSet<Integer> hours = new TreeSet<>();
		TreeSet<Integer> days = new TreeSet<>();
		TreeSet<Integer> weeks = new TreeSet<>();

		for (LocalDateTime published : publishedAt) {
			if (published == null) {
				continue;
			}
			ZonedDateTime zoned = published.atZone(STORED_ZONE).withZoneSameInstant(ZONE);
			if (zoned.isBefore(yearStart) || !zoned.isBefore(nextYear)) {
				continue;
			}
			weeks.add(weekBucket(zoned.toLocalDate()));
			if (!zoned.isBefore(monthStart) && zoned.isBefore(monthStart.plusMonths(1))) {
				days.add(zoned.getDayOfMonth() - 1);
			}
			if (!zoned.isBefore(dayStart) && zoned.isBefore(dayStart.plusDays(1))) {
				hours.add(zoned.getHour());
			}
		}

		return new MomentCadenceResponse(ZONE.getId(), new DayCadence(dayStart.toOffsetDateTime(), List.copyOf(hours)),
				new MonthCadence(month.atDay(1), month.lengthOfMonth(), sundayBasedOffset(month.atDay(1)),
						List.copyOf(days)),
				new YearCadence(today.getYear(), List.copyOf(weeks)));
	}

	/**
	 * Maps a date to one of four week buckets inside its month.
	 * 
	 * @param date
	 *            publication date in the author time zone
	 * @return zero-based bucket from {@code 0} through {@code 47}
	 */
	static int weekBucket(LocalDate date) {
		int week = Math.min((date.getDayOfMonth() - 1) / 7, 3);
		return (date.getMonthValue() - 1) * 4 + week;
	}

	/**
	 * Returns the first stored UTC timestamp required to cover the current author
	 * year.
	 * 
	 * @param now
	 *            instant that selects the current year
	 * @return inclusive lower bound for stored publication timestamps
	 */
	public static LocalDateTime storedRangeStart(ZonedDateTime now) {
		LocalDate firstDay = now.withZoneSameInstant(ZONE).toLocalDate().withDayOfYear(1);
		return firstDay.atStartOfDay(ZONE).withZoneSameInstant(STORED_ZONE).toLocalDateTime();
	}

	/**
	 * Returns the first stored UTC timestamp after the current author year.
	 * 
	 * @param now
	 *            instant that selects the current year
	 * @return exclusive upper bound for stored publication timestamps
	 */
	public static LocalDateTime storedRangeEnd(ZonedDateTime now) {
		LocalDate nextYear = now.withZoneSameInstant(ZONE).toLocalDate().withDayOfYear(1).plusYears(1);
		return nextYear.atStartOfDay(ZONE).withZoneSameInstant(STORED_ZONE).toLocalDateTime();
	}

	private static int sundayBasedOffset(LocalDate firstDay) {
		return firstDay.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : firstDay.getDayOfWeek().getValue();
	}
}
