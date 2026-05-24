package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

@Builder
@Schema(description = "Professional overview of website traffic and performance")
public record AnalyticsOverviewResponse(
		@Schema(description = "Total Page Views today") long todayPv,
		@Schema(description = "Total Unique Visitors today") long todayUv,
		@Schema(description = "Total Page Views yesterday") long yesterdayPv,
		@Schema(description = "Total Unique Visitors yesterday") long yesterdayUv,
		@Schema(description = "Growth rate of PV compared to yesterday (%)") double pvGrowthRate,
		@Schema(description = "Daily traffic trends for the last 7 days") List<VisitTrendItem> dailyTrends,
		@Schema(description = "Ranking of top visited content") List<TopContentItem> topContent
) implements Serializable {
	private static final long serialVersionUID = 1L;

	@Builder
	public record VisitTrendItem(
			@Schema(description = "Date in YYYY-MM-DD format") String date,
			@Schema(description = "Page Views") long pv,
			@Schema(description = "Unique Visitors") long uv
	) implements Serializable {}

	@Builder
	public record TopContentItem(
			@Schema(description = "Content URL or Title") String title,
			@Schema(description = "Page path") String url,
			@Schema(description = "View count") long count
	) implements Serializable {}
}
