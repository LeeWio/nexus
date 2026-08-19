package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

@Builder
@Schema(description = "Traffic statistics including device and source breakdown")
public record TrafficStatsResponse(
		@Schema(description = "Detailed summary cards for the top of the dashboard") SummaryMetrics summary,

		@Schema(description = "Daily traffic time series for line charts") List<TimeSeriesItem> timeSeries,

		@Schema(description = "Traffic by device type") List<TrafficMetric> devices,

		@Schema(description = "Traffic by source channel") List<TrafficMetric> sources) implements Serializable {
	private static final long serialVersionUID = 1L;

	@Builder
	public record SummaryMetrics(@Schema(description = "Total sessions and its growth rate") Metric sessions,
			@Schema(description = "Total unique users and its growth rate") Metric users,
			@Schema(description = "Visitors who also visited before this reporting window") Metric returningVisitors,
			@Schema(description = "Bounce rate and its growth rate") Metric bounceRate,
			@Schema(description = "Average session duration (e.g. 3m 42s) and its growth rate") Metric avgSession)
			implements
				Serializable {
	}

	@Builder
	public record Metric(
			@Schema(description = "Current value as a string (e.g. '84,210', '41.3%', '3m 42s')") String value,
			@Schema(description = "Numeric value for raw calculations") double numericValue,
			@Schema(description = "Growth rate percentage (positive or negative)") double growthRate)
			implements
				Serializable {
	}

	@Builder
	public record TimeSeriesItem(@Schema(description = "Date string (e.g., Oct 21)") String date,
			@Schema(description = "Sessions (PV)") long sessions,
			@Schema(description = "Users (UV)") long users) implements Serializable {
	}

	@Builder
	public record TrafficMetric(
			@Schema(description = "Name of the metric (e.g., Mobile, Desktop, Tablet, Direct, Search)") String name,

			@Schema(description = "Number of views") long views,

			@Schema(description = "Percentage of total (0.0 to 100.0)") double percentage) implements Serializable {
	}
}
