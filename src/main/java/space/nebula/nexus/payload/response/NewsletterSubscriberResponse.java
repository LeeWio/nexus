package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import space.nebula.nexus.enums.SubscriberStatus;

import java.time.LocalDateTime;

/** Safe administrative representation of one newsletter subscriber. */
@Schema(description = "Newsletter subscriber visible to administrators")
public record NewsletterSubscriberResponse(@Schema(description = "Subscriber ID") Long id,
		@Schema(description = "Subscriber email address") String email,
		@Schema(description = "Subscription lifecycle state") SubscriberStatus status,
		@Schema(description = "Time the subscription was requested") LocalDateTime createdAt,
		@Schema(description = "Time the subscription was verified; null until active") LocalDateTime verifiedAt) {
}
