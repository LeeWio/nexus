package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;

public interface IInteractionService
{

	ApiResponse<Void> likePost(Long postId);

	ApiResponse<Void> unlikePost(Long postId);

	ApiResponse<Void> likeComment(Long commentId);

	ApiResponse<Void> unlikeComment(Long commentId);

	ApiResponse<Void> favoritePost(Long postId);

	ApiResponse<Void> unfavoritePost(Long postId);

	/**
	 * Updates a PostResponse with dynamic interaction data (like/favorite status)
	 * for the current user.
	 */
	void populateInteractionData(space.nebula.nexus.payload.response.PostResponse.PostResponseBuilder builder,
			Long postId);
}
