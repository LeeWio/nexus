package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostReviewRequest(
		@NotNull(message = "Approved status is required") Boolean approved,
		@Size(max = 1000, message = "Review comment must not exceed 1000 characters") String reviewComment
) {}
