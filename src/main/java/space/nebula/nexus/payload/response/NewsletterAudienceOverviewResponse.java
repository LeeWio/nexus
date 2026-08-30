package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregate subscriber counts intended for the administrative audience view.
 */
@Schema(description = "Newsletter subscriber audience overview")
public record NewsletterAudienceOverviewResponse(
		@Schema(description = "Active, verified subscribers") long activeSubscribers,
		@Schema(description = "Subscribers awaiting email verification") long pendingSubscribers,
		@Schema(description = "Subscribers who opted out") long unsubscribedSubscribers,
		@Schema(description = "Active subscribers verified during the last 30 days") long verifiedLast30Days) {
}
