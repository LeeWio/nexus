package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Admin-facing conversion metrics from a post impression through engagement.
 */
@Builder
@Schema(description = "Content discovery and conversion funnel")
public record ContentFunnelResponse(@Schema(description = "Applied optional post filter") Long postId,
		@Schema(description = "Start of the reporting window") LocalDateTime start,
		@Schema(description = "End of the reporting window") LocalDateTime end, long impressions, long clicks,
		long readers25Percent, long readers50Percent, long readers75Percent, long completedReads, long likes,
		long favorites, long verifiedSubscriptions, long returningVisitors,
		@Schema(description = "Average active seconds reported by completed reads") double averageActiveReadSeconds,
		@Schema(description = "Click-through rate from impression to click") double clickThroughRate,
		@Schema(description = "Completion rate from click to completed read") double completionRate)
		implements
			Serializable {
	private static final long serialVersionUID = 1L;
}
