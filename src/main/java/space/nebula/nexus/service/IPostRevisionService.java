package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.PostRevisionKind;
import space.nebula.nexus.payload.response.PostDiffResponse;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.PostRevisionDetailResponse;
import space.nebula.nexus.payload.response.PostRevisionResponse;
import space.nebula.nexus.payload.response.PostRevisionSummaryResponse;

import java.util.List;

public interface IPostRevisionService {

	/**
	 * Saves an immutable revision in the same transaction as the post mutation.
	 */
	void saveRevision(Post post, PostRevisionKind revisionKind, String changeSummary);

	/**
	 * Rejects stale editor writes when an expected revision number is supplied.
	 */
	void assertExpectedRevision(Long postId, Integer expectedRevisionNumber);

	/**
	 * Gets all revisions for a specific post, ordered by version descending.
	 */
	ApiResponse<List<PostRevisionResponse>> getPostRevisions(Long postId);

	/**
	 * Gets a compact revision timeline without loading article bodies.
	 */
	ApiResponse<List<PostRevisionSummaryResponse>> getPostRevisionSummaries(Long postId);

	/**
	 * Gets the complete immutable snapshot for one revision.
	 */
	ApiResponse<PostRevisionDetailResponse> getPostRevision(Long postId, Long revisionId);

	/**
	 * Reverts a post to a specific revision.
	 */
	ApiResponse<PostResponse> revertToRevision(Long postId, Long revisionId, Integer expectedRevisionNumber);

	/**
	 * Compares two revisions of a post.
	 */
	ApiResponse<PostDiffResponse> compareRevisions(Long postId, Long baseRevisionId, Long targetRevisionId);
}
