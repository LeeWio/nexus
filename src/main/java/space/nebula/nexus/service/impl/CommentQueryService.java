package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.response.CommentAnchorContextResponse;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.CursorPageResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentQueryService
{

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final CommentResponseAssembler commentResponseAssembler;

	@Transactional(readOnly = true)
	public ApiResponse<List<Tree<Long>>> retrieveCommentsByPost(Long postId)
	{
		validatePublishedPost(postId);

		var comments = commentRepository.findAllByPostIdAndStatusOrderByPathAsc(postId, CommentStatus.APPROVED);
		return ApiResponse.success(buildCommentTree(commentResponseAssembler.toResponseList(comments)));
	}

	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> retrieveRootCommentsByPost(Long postId, Pageable pageable)
	{
		validatePublishedPost(postId);

		var comments = commentRepository.findAllByPostIdAndParentIsNullAndStatus(postId, CommentStatus.APPROVED,
				pageable);
		return ApiResponse.success(toPageResult(comments));
	}

	@Transactional(readOnly = true)
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveRootCommentsByPostCursor(Long postId, Long cursor,
			int size)
	{
		validatePublishedPost(postId);
		Pageable limit = cursorLimit(size);
		List<Comment> comments = cursor == null
				? commentRepository.findAllByPostIdAndParentIsNullAndStatusOrderByIdDesc(postId,
						CommentStatus.APPROVED, limit)
				: commentRepository.findAllByPostIdAndParentIsNullAndStatusAndIdLessThanOrderByIdDesc(postId,
						CommentStatus.APPROVED, cursor, limit);
		return ApiResponse.success(toCursorResponse(comments, size));
	}

	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> retrieveReplies(Long parentId, Pageable pageable)
	{
		validateVisibleReplyParent(parentId);

		var replies = commentRepository.findAllByParentIdAndStatus(parentId, CommentStatus.APPROVED, pageable);
		return ApiResponse.success(toPageResult(replies));
	}

	@Transactional(readOnly = true)
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveRepliesCursor(Long parentId, Long cursor, int size)
	{
		validateVisibleReplyParent(parentId);
		Pageable limit = cursorLimit(size);
		List<Comment> replies = cursor == null
				? commentRepository.findAllByParentIdAndStatusOrderByIdAsc(parentId, CommentStatus.APPROVED, limit)
				: commentRepository.findAllByParentIdAndStatusAndIdGreaterThanOrderByIdAsc(parentId,
						CommentStatus.APPROVED, cursor, limit);
		return ApiResponse.success(toCursorResponse(replies, size));
	}

	@Transactional(readOnly = true)
	public ApiResponse<List<Tree<Long>>> retrieveGuestbookComments()
	{
		var comments = commentRepository.findAllByPostIsNullAndStatusOrderByPathAsc(CommentStatus.APPROVED);
		return ApiResponse.success(buildCommentTree(commentResponseAssembler.toResponseList(comments)));
	}

	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> retrieveGuestbookRootComments(Pageable pageable)
	{
		var comments = commentRepository.findAllByPostIsNullAndParentIsNullAndStatus(CommentStatus.APPROVED, pageable);
		return ApiResponse.success(toPageResult(comments));
	}

	@Transactional(readOnly = true)
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveGuestbookRootCommentsCursor(Long cursor, int size)
	{
		Pageable limit = cursorLimit(size);
		List<Comment> comments = cursor == null
				? commentRepository.findAllByPostIsNullAndParentIsNullAndStatusOrderByIdDesc(CommentStatus.APPROVED,
						limit)
				: commentRepository.findAllByPostIsNullAndParentIsNullAndStatusAndIdLessThanOrderByIdDesc(
						CommentStatus.APPROVED, cursor, limit);
		return ApiResponse.success(toCursorResponse(comments, size));
	}

	@Transactional(readOnly = true)
	public ApiResponse<Long> countNewRootCommentsByPost(Long postId, Long afterId)
	{
		validatePublishedPost(postId);
		long count = commentRepository.countByPostIdAndParentIsNullAndStatusAndIdGreaterThan(postId,
				CommentStatus.APPROVED, normalizeCursor(afterId));
		return ApiResponse.success(count);
	}

	@Transactional(readOnly = true)
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveNewRootCommentsByPost(Long postId, Long afterId,
			int size)
	{
		validatePublishedPost(postId);
		List<Comment> comments = commentRepository.findNewRootCommentsByPost(postId, CommentStatus.APPROVED,
				normalizeCursor(afterId), cursorLimit(size));
		return ApiResponse.success(toForwardCursorResponse(comments, size));
	}

	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> retrieveHotRootCommentsByPost(Long postId, Pageable pageable)
	{
		validatePublishedPost(postId);
		var comments = commentRepository.findHotRootCommentsByPost(postId, CommentStatus.APPROVED, pageable);
		return ApiResponse.success(toPageResult(comments));
	}

	@Transactional(readOnly = true)
	public ApiResponse<CommentAnchorContextResponse> retrieveCommentAnchorContext(Long commentId, int replyWindowSize)
	{
		Assert.isTrue(replyWindowSize >= 1 && replyWindowSize <= 100,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Reply window size must be between 1 and 100"));
		Comment target = commentRepository.findById(commentId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
		Assert.isTrue(target.getStatus() == CommentStatus.APPROVED,
				() -> new BusinessException(BusinessCode.FORBIDDEN, "Hidden comments cannot be anchored"));
		Comment root = resolveRootComment(target);
		if (root.getPost() != null)
		{
			Assert.isTrue(root.getPost().getStatus() == PostStatus.PUBLISHED,
					() -> new BusinessException(BusinessCode.FORBIDDEN,
							"Comments are not available for unpublished posts"));
		}
		var replies = commentRepository.findAllByParentIdAndStatus(root.getId(), CommentStatus.APPROVED,
				PageRequest.of(0, replyWindowSize));
		return ApiResponse.success(CommentAnchorContextResponse.builder()
				.rootCommentId(root.getId())
				.rootComment(commentResponseAssembler.toResponse(root))
				.targetComment(commentResponseAssembler.toResponse(target))
				.repliesWindow(toPageResult(replies))
				.build());
	}

	@Transactional(readOnly = true)
	public ApiResponse<Long> countNewGuestbookRootComments(Long afterId)
	{
		long count = commentRepository.countByPostIsNullAndParentIsNullAndStatusAndIdGreaterThan(CommentStatus.APPROVED,
				normalizeCursor(afterId));
		return ApiResponse.success(count);
	}

	@Transactional(readOnly = true)
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveNewGuestbookRootComments(Long afterId, int size)
	{
		List<Comment> comments = commentRepository.findNewGuestbookRootComments(CommentStatus.APPROVED,
				normalizeCursor(afterId), cursorLimit(size));
		return ApiResponse.success(toForwardCursorResponse(comments, size));
	}

	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> retrieveHotGuestbookRootComments(Pageable pageable)
	{
		var comments = commentRepository.findHotGuestbookRootComments(CommentStatus.APPROVED, pageable);
		return ApiResponse.success(toPageResult(comments));
	}

	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> searchCommentsForManagement(CommentStatus status, Long postId,
			String username, String keyword, Pageable pageable)
	{
		var spec = space.nebula.nexus.repository.specification.CommentSpecification.filterComments(status, postId,
				username, keyword);
		var comments = commentRepository.findAll(spec, pageable);
		return ApiResponse.success(toPageResult(comments));
	}

	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> retrievePendingComments(Pageable pageable)
	{
		var comments = commentRepository.findAllByStatus(CommentStatus.PENDING, pageable);
		return ApiResponse.success(toPageResult(comments));
	}

	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> retrieveMyComments(CommentStatus status, Pageable pageable)
	{
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		var comments = status == null ? commentRepository.findAllByUserId(currentUser.getId(), pageable)
				: commentRepository.findAllByUserIdAndStatus(currentUser.getId(), status, pageable);
		return ApiResponse.success(toPageResult(comments));
	}

	private void validatePublishedPost(Long postId)
	{
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
		Assert.isTrue(post.getStatus() == PostStatus.PUBLISHED,
				() -> new BusinessException(BusinessCode.FORBIDDEN,
						"Comments are not available for unpublished posts"));
	}

	private void validateVisibleReplyParent(Long parentId)
	{
		Comment parent = commentRepository.findById(parentId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", parentId));
		Assert.isTrue(parent.getStatus() == CommentStatus.APPROVED,
				() -> new BusinessException(BusinessCode.FORBIDDEN, "Replies are not available for hidden comments"));
		if (parent.getPost() != null)
		{
			Assert.isTrue(parent.getPost().getStatus() == PostStatus.PUBLISHED,
					() -> new BusinessException(BusinessCode.FORBIDDEN,
							"Replies are not available for unpublished posts"));
		}
	}

	private Pageable cursorLimit(int size)
	{
		Assert.isTrue(size >= 1 && size <= 100,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Cursor page size must be between 1 and 100"));
		return PageRequest.of(0, size + 1);
	}

	private Long normalizeCursor(Long cursor)
	{
		return cursor == null ? 0L : cursor;
	}

	private CursorPageResponse<CommentResponse> toCursorResponse(List<Comment> comments, int size)
	{
		boolean hasMore = comments.size() > size;
		List<Comment> window = hasMore ? comments.subList(0, size) : comments;
		Long nextCursor = hasMore && !window.isEmpty() ? window.getLast().getId() : null;
		return CursorPageResponse.<CommentResponse>builder()
				.list(commentResponseAssembler.toResponseList(window))
				.nextCursor(nextCursor)
				.hasMore(hasMore)
				.build();
	}

	private CursorPageResponse<CommentResponse> toForwardCursorResponse(List<Comment> comments, int size)
	{
		boolean hasMore = comments.size() > size;
		List<Comment> window = hasMore ? comments.subList(0, size) : comments;
		Long nextCursor = hasMore && !window.isEmpty() ? window.getLast().getId() : null;
		return CursorPageResponse.<CommentResponse>builder()
				.list(commentResponseAssembler.toResponseList(window))
				.nextCursor(nextCursor)
				.hasMore(hasMore)
				.build();
	}

	private Comment resolveRootComment(Comment comment)
	{
		Comment current = comment;
		while (current.getParent() != null)
		{
			current = current.getParent();
		}
		return current;
	}

	private PageResult<CommentResponse> toPageResult(Page<Comment> page)
	{
		return new PageResult<>(commentResponseAssembler.toResponseList(page.getContent()), page.getTotalElements(),
				page.getNumber() + 1, page.getSize(), page.getTotalPages());
	}

	private List<Tree<Long>> buildCommentTree(List<CommentResponse> flatComments)
	{
		TreeNodeConfig config = new TreeNodeConfig();
		config.setIdKey("id");
		config.setParentIdKey("parentId");
		config.setWeightKey("id");

		return TreeUtil.build(flatComments, null, config, (commentResponse, treeNode) ->
		{
			treeNode.setId(commentResponse.id());
			treeNode.setParentId(commentResponse.parentId());
			treeNode.putExtra("content", commentResponse.content());
			treeNode.putExtra("username", commentResponse.username());
			treeNode.putExtra("avatar", commentResponse.avatar());
			treeNode.putExtra("likesCount", commentResponse.likesCount());
			treeNode.putExtra("replyCount", commentResponse.replyCount());
			treeNode.putExtra("likedByCurrentUser", commentResponse.likedByCurrentUser());
			treeNode.putExtra("createdAt", commentResponse.createdAt());
			treeNode.putExtra("editedAt", commentResponse.editedAt());
		});
	}
}
