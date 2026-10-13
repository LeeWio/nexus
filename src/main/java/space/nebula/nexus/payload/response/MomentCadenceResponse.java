package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Publishing cadence of public moments for the current day, month, and year.
 *
 * <p>
 * Each {@code active} list contains the zero-based buckets that contain at
 * least one public moment. Repeated publications inside one bucket stay one
 * active bucket.
 */
@Schema(description = "Public moment publishing cadence for the current day, month, and year")
public record MomentCadenceResponse(
		@Schema(description = "IANA time zone used to place each publication", example = "Asia/Shanghai") String zone,
		@Schema(description = "Hours of the current day that contain a public moment") DayCadence day,
		@Schema(description = "Days of the current month that contain a public moment") MonthCadence month,
		@Schema(description = "Months of the current year that contain a public moment") YearCadence year)
		implements
			Serializable {

	private static final long serialVersionUID = 1L;

	public MomentCadenceResponse {
		day = copy(day);
		month = copy(month);
		year = copy(year);
	}

	/**
	 * One hour of the current calendar day.
	 *
	 * @param start
	 *            start of the current day in the cadence time zone
	 * @param active
	 *            zero-based hours, from {@code 0} through {@code 23}, that contain
	 *            a public moment
	 */
	@Schema(description = "Hourly publishing cadence for the current day")
	public record DayCadence(
			@Schema(description = "Start of the current day in the cadence time zone", example = "2026-10-13T00:00:00+08:00") OffsetDateTime start,
			@Schema(description = "Zero-based hours containing a public moment", example = "[0, 8, 21]") List<Integer> active)
			implements
				Serializable {

		private static final long serialVersionUID = 1L;

		public DayCadence {
			active = List.copyOf(active);
		}
	}

	/**
	 * One day of the current calendar month.
	 *
	 * @param start
	 *            first day of the current month
	 * @param days
	 *            number of days in the current month, from 28 through 31
	 * @param offset
	 *            Sunday-based weekday of the first day, from {@code 0} through
	 *            {@code 6}
	 * @param active
	 *            zero-based days, from {@code 0}, that contain a public moment
	 */
	@Schema(description = "Daily publishing cadence for the current month")
	public record MonthCadence(
			@Schema(description = "First day of the current month", example = "2026-10-01") LocalDate start,
			@Schema(description = "Number of days in the current month", example = "31", minimum = "28", maximum = "31") int days,
			@Schema(description = "Sunday-based weekday of the first day", example = "4", minimum = "0", maximum = "6") int offset,
			@Schema(description = "Zero-based days containing a public moment", example = "[0, 14]") List<Integer> active)
			implements
				Serializable {

		private static final long serialVersionUID = 1L;

		public MonthCadence {
			active = List.copyOf(active);
		}
	}

	/**
	 * Weeks of the current calendar year.
	 *
	 * <p>
	 * Each month contributes four week buckets. A fifth calendar week is folded
	 * into the fourth bucket, so the grid stays at 48 cells.
	 *
	 * @param year
	 *            calendar year represented by the cadence
	 * @param active
	 *            zero-based week buckets, from {@code 0} through {@code 47}
	 */
	@Schema(description = "Weekly publishing cadence for the current year")
	public record YearCadence(
			@Schema(description = "Calendar year represented by the cadence", example = "2026") int year,
			@Schema(description = "Zero-based week buckets containing a public moment", example = "[0, 35]") List<Integer> active)
			implements
				Serializable {

		private static final long serialVersionUID = 1L;

		public YearCadence {
			active = List.copyOf(active);
		}
	}

	private static DayCadence copy(DayCadence value) {
		return new DayCadence(value.start(), value.active());
	}

	private static MonthCadence copy(MonthCadence value) {
		return new MonthCadence(value.start(), value.days(), value.offset(), value.active());
	}

	private static YearCadence copy(YearCadence value) {
		return new YearCadence(value.year(), value.active());
	}
}
