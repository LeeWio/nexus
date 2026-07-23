package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * User-controlled recommendation preferences.
 *
 * @param followedCategories
 *            explicitly followed categories
 * @param hiddenPostCount
 *            number of posts hidden from recommendations
 */
@Schema(description = "User-controlled recommendation preferences")
public record ContentPreferenceResponse(
		@Schema(description = "Explicitly followed categories") List<CategoryResponse> followedCategories,
		@Schema(description = "Number of hidden recommendation posts") long hiddenPostCount) implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates an immutable preference response.
	 */
	public ContentPreferenceResponse {
		followedCategories = List.copyOf(followedCategories);
	}
}
