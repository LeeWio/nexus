package space.nebula.nexus.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;

@Builder
@Schema(description = "Top pages analytics data")
public record TopPageResponse(
		@Schema(description = "Page path")
		String path,
		
		@Schema(description = "Page views")
		long views,
		
		@Schema(description = "Average time on page (formatted string or seconds)")
		@JsonProperty("avs.time")
		String avgTime,
		
		@Schema(description = "Bounce rate percentage (e.g. 45.5)")
		@JsonProperty("bounce")
		double bounceRate,
		
		@Schema(description = "Trend compared to previous period (+/- percentage)")
		String trend
) implements Serializable {
	private static final long serialVersionUID = 1L;
}
