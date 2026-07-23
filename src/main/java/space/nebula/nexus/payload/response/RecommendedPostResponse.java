package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Personalized post recommendation with a user-facing explanation.
 *
 * @param post
 *            recommended post
 * @param reasonCode
 *            stable reason identifier for frontend presentation logic
 * @param reason
 *            concise English explanation shown to the user
 */
@Schema(description = "Personalized post recommendation")
public record RecommendedPostResponse(@Schema(description = "Recommended post") PostDigestResponse post,
		@Schema(description = "Stable recommendation reason code", example = "CATEGORY_INTEREST") String reasonCode,
		@Schema(description = "User-facing recommendation explanation", example = "Recommended because you often read Architecture.") String reason)
		implements
			Serializable {
	private static final long serialVersionUID = 1L;
}
