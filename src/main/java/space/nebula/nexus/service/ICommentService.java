package space.nebula.nexus.service;

import cn.hutool.core.lang.tree.Tree;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.PageResult;

import java.util.List;

public interface ICommentService
{

	/**
	 * Publishes a new comment. Performs auto-moderation.
	 */
	ApiResponse<Void> publishComment(CommentRequest request, HttpServletRequest servletRequest);

	/**
	 * Retrieves approved comments for a specific post as a tree.
	 */
	ApiResponse<List<Tree<Long>>> retrieveCommentsByPost(Long postId);

	/**
	 * Retrieves approved comments for the guestbook as a tree.
	 */
	ApiResponse<List<Tree<Long>>> retrieveGuestbookComments();

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
	 * Moderates a comment by updating its status.
	 *
	 * @param id comment identifier
	 * @param status approved or rejected moderation outcome
	 * @return successful response after the moderation decision is persisted
	 */
	ApiResponse<Void> moderateComment(Long id, CommentStatus status);

	/**
	 * Soft-deletes a comment that has no active replies.
	 *
	 * @param id comment identifier
	 * @return successful response after the comment is archived
	 */
	ApiResponse<Void> deleteComment(Long id);
}
