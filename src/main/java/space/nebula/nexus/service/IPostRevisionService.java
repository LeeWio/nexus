package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.payload.response.PostDiffResponse;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.PostRevisionResponse;

import java.util.List;

public interface IPostRevisionService {

	/**
	 * Saves a snapshot of the given post as a new revision.
	 */
	void saveRevision(Post post);

	void saveRevision(Post post, String changeType, String changeSummary);

	/**
	 * Gets all revisions for a specific post, ordered by version descending.
	 */
	ApiResponse<List<PostRevisionResponse>> getPostRevisions(Long postId);

	/**
	 * Reverts a post to a specific revision.
	 */
	ApiResponse<PostResponse> revertToRevision(Long postId, Long revisionId);

	/**
	 * Compares two revisions of a post.
	 */
	ApiResponse<PostDiffResponse> compareRevisions(Long postId, Long baseRevisionId, Long targetRevisionId);
}
