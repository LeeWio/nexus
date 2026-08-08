package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.CommentInteractionResponse;
import space.nebula.nexus.payload.response.PostInteractionResponse;

public interface IInteractionService {

	ApiResponse<PostInteractionResponse> likePost(Long postId);

	ApiResponse<PostInteractionResponse> unlikePost(Long postId);

	ApiResponse<CommentInteractionResponse> likeComment(Long commentId);

	ApiResponse<CommentInteractionResponse> unlikeComment(Long commentId);

	ApiResponse<PostInteractionResponse> favoritePost(Long postId);

	ApiResponse<PostInteractionResponse> unfavoritePost(Long postId);

	/**
	 * Updates a PostResponse with dynamic interaction data (like/favorite status)
	 * for the current user.
	 */
	void populateInteractionData(space.nebula.nexus.payload.response.PostResponse.PostResponseBuilder builder,
			Long postId);
}
