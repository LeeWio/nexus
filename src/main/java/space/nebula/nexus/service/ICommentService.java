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

public interface ICommentService {

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
