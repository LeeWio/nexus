package space.nebula.nexus.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.PageResult;

public interface ICommentService {

	/**
	 * Publishes a new comment. Performs auto-moderation.
	 */
	ApiResponse<Void> publishComment(CommentRequest request, HttpServletRequest servletRequest);

	/**
	 * Retrieves approved comments for a specific post.
	 */
	ApiResponse<PageResult<CommentResponse>> retrieveCommentsByPost(Long postId, Pageable pageable);

	/**
	 * Retrieves approved comments for the guestbook.
	 */
	ApiResponse<PageResult<CommentResponse>> retrieveGuestbookComments(Pageable pageable);

	/**
	 * Searches all comments for administrative management.
	 */
	ApiResponse<PageResult<CommentResponse>> searchCommentsForManagement(Pageable pageable);

	/**
	 * Retrieves comments that are pending moderation.
	 */
	ApiResponse<PageResult<CommentResponse>> retrievePendingComments(Pageable pageable);

	/**
	 * Moderates a comment by updating its status.
	 */
	ApiResponse<Void> moderateComment(Long id, CommentStatus status);

	/**
	 * Performs a hard/soft delete of a comment.
	 */
	ApiResponse<Void> deleteComment(Long id);
}
