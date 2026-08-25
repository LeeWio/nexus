package space.nebula.nexus.service;

import cn.hutool.core.lang.tree.Tree;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentReportStatus;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.request.CommentReportRequest;
import space.nebula.nexus.payload.request.CommentUpdateRequest;
import space.nebula.nexus.payload.response.CommentAnchorContextResponse;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.CommentPublishResponse;
import space.nebula.nexus.payload.response.CommentGovernanceOverviewResponse;
import space.nebula.nexus.payload.response.CommentModerationLogResponse;
import space.nebula.nexus.payload.response.CommentRiskResponse;
import space.nebula.nexus.payload.response.CommentReportResponse;
import space.nebula.nexus.payload.response.CursorPageResponse;
import space.nebula.nexus.payload.response.PageResult;

import java.util.List;

public interface ICommentService {

	/**
	 * Publishes a new comment. Performs auto-moderation.
	 */
	ApiResponse<CommentPublishResponse> publishComment(CommentRequest request, HttpServletRequest servletRequest);

	/**
	 * Retrieves approved comments for a specific post as a tree.
	 */
	ApiResponse<List<Tree<Long>>> retrieveCommentsByPost(Long postId);

	/**
	 * Retrieves root-level approved comments for a specific post with pagination.
	 */
	ApiResponse<PageResult<CommentResponse>> retrieveRootCommentsByPost(Long postId, Pageable pageable);

	/**
	 * Retrieves root-level approved comments for a post using cursor loading.
	 */
	ApiResponse<CursorPageResponse<CommentResponse>> retrieveRootCommentsByPostCursor(Long postId, Long cursor,
			int size);

	/**
	 * Retrieves approved child replies for a parent comment with pagination.
	 */
	ApiResponse<PageResult<CommentResponse>> retrieveReplies(Long parentId, Pageable pageable);

	/**
	 * Retrieves approved child replies using cursor loading.
	 */
	ApiResponse<CursorPageResponse<CommentResponse>> retrieveRepliesCursor(Long parentId, Long cursor, int size);

	/**
	 * Retrieves approved comments for the guestbook as a tree.
	 */
	ApiResponse<List<Tree<Long>>> retrieveGuestbookComments();

	/**
	 * Retrieves root-level approved guestbook comments with pagination.
	 */
	ApiResponse<PageResult<CommentResponse>> retrieveGuestbookRootComments(Pageable pageable);

	/**
	 * Retrieves root-level approved guestbook comments using cursor loading.
	 */
	ApiResponse<CursorPageResponse<CommentResponse>> retrieveGuestbookRootCommentsCursor(Long cursor, int size);

	/**
	 * Counts root-level approved comments newer than a client anchor.
	 */
	ApiResponse<Long> countNewRootCommentsByPost(Long postId, Long afterId);

	ApiResponse<CursorPageResponse<CommentResponse>> retrieveNewRootCommentsByPost(Long postId, Long afterId, int size);

	ApiResponse<PageResult<CommentResponse>> retrieveHotRootCommentsByPost(Long postId, Pageable pageable);

	ApiResponse<CommentAnchorContextResponse> retrieveCommentAnchorContext(Long commentId, int replyWindowSize);

	/**
	 * Counts root-level approved guestbook comments newer than a client anchor.
	 */
	ApiResponse<Long> countNewGuestbookRootComments(Long afterId);

	ApiResponse<CursorPageResponse<CommentResponse>> retrieveNewGuestbookRootComments(Long afterId, int size);

	ApiResponse<PageResult<CommentResponse>> retrieveHotGuestbookRootComments(Pageable pageable);

	/**
	 * Retrieves comments authored by the current user with optional status
	 * filtering.
	 */
	ApiResponse<PageResult<CommentResponse>> retrieveMyComments(CommentStatus status, Pageable pageable);

	/**
	 * Withdraws a non-public comment authored by the current user.
	 */
	ApiResponse<Void> withdrawMyComment(Long id);

	/**
	 * Updates a comment authored by the current user.
	 */
	ApiResponse<Void> updateMyComment(Long id, CommentUpdateRequest request);

	/**
	 * Deletes a comment authored by the current user when the thread can stay
	 * consistent.
	 */
	ApiResponse<Void> deleteMyComment(Long id);

	/**
	 * Reports an approved public comment for moderation review.
	 */
	ApiResponse<Void> reportComment(Long id, CommentReportRequest request);

	/**
	 * Searches all comments for administrative management with filters.
	 */
	ApiResponse<PageResult<CommentResponse>> searchCommentsForManagement(CommentStatus status, Long postId,
			String username, String keyword, Pageable pageable);

	/**
	 * Retrieves comments that are pending moderation.
	 */
	ApiResponse<PageResult<CommentResponse>> retrievePendingComments(Pageable pageable);

	/**
	 * Retrieves moderation dashboard counters and recent governance activity.
	 */
	ApiResponse<CommentGovernanceOverviewResponse> retrieveCommentGovernanceOverview();

	/**
	 * Retrieves comment reports for the moderation console.
	 */
	ApiResponse<PageResult<CommentReportResponse>> retrieveCommentReports(CommentReportStatus status, Long commentId,
			String reporterUsername, Pageable pageable);

	/**
	 * Retrieves comments that should be prioritized by moderators.
	 */
	ApiResponse<PageResult<CommentRiskResponse>> retrieveHighRiskComments(Long minOpenReports, Pageable pageable);

	/**
	 * Retrieves immutable moderation history for audit and operations.
	 */
	ApiResponse<PageResult<CommentModerationLogResponse>> retrieveCommentModerationLogs(Long commentId,
			CommentModerationAction action, Pageable pageable);

	ApiResponse<Integer> repairCommentCounters();

	/**
	 * Moderates a comment by updating its status.
	 *
	 * @param id
	 *            comment identifier
	 * @param status
	 *            approved or rejected moderation outcome
	 * @return successful response after the moderation decision is persisted
	 */
	ApiResponse<Void> moderateComment(Long id, CommentStatus status);

	/**
	 * Applies the same moderation decision to multiple comments.
	 */
	ApiResponse<Integer> batchModerateComments(List<Long> ids, CommentStatus status);

	ApiResponse<Void> pinComment(Long id, boolean pinned);

	ApiResponse<Void> featureComment(Long id, boolean featured);

	/**
	 * Soft-deletes a comment that has no active replies.
	 *
	 * @param id
	 *            comment identifier
	 * @return successful response after the comment is archived
	 */
	ApiResponse<Void> deleteComment(Long id);
}
