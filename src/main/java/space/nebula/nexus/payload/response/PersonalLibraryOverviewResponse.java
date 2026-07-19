package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * Aggregated personal content surface for the authenticated user.
 *
 * @param continueReading incomplete reading sessions ordered by recent activity
 * @param recentFavorites recently favorited posts not already shown in continue reading
 * @param recommendations unseen posts selected from user interests or community popularity
 */
@Schema(description = "Personalized content library overview")
public record PersonalLibraryOverviewResponse(
		@Schema(description = "Incomplete reading sessions") List<ReadingHistoryResponse> continueReading,
		@Schema(description = "Recently favorited posts") List<FavoritePostResponse> recentFavorites,
		@Schema(description = "Explainable post recommendations") List<RecommendedPostResponse> recommendations)
		implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Creates an immutable overview response.
	 */
	public PersonalLibraryOverviewResponse
	{
		continueReading = List.copyOf(continueReading);
		recentFavorites = List.copyOf(recentFavorites);
		recommendations = List.copyOf(recommendations);
	}
}
