package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotNull;

public record PostReviewRequest(
		@NotNull(message = "Approved status is required") Boolean approved,
		String reviewComment
) {}
