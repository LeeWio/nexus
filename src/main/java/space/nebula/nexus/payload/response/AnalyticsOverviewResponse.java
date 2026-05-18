package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Overview of website traffic statistics")
public record AnalyticsOverviewResponse(
    @Schema(description = "Total Page Views today")
    long todayPv,
    
    @Schema(description = "Total Unique Visitors today")
    long todayUv,
    
    @Schema(description = "Total Page Views yesterday")
    long yesterdayPv,
    
    @Schema(description = "Total Unique Visitors yesterday")
    long yesterdayUv,
    
    @Schema(description = "Growth rate of PV compared to yesterday (%)")
    double pvGrowthRate,
    
    @Schema(description = "Ranking of top visited content")
    List<Map<String, Object>> topContent
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
