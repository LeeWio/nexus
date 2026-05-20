package space.nebula.nexus.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.CommentMapper;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.ICommentService;
import space.nebula.nexus.service.SensitiveWordService;
import space.nebula.nexus.utils.IpUtil;

/**
 * Professional implementation of ICommentService with hierarchical support,
 * moderation, sensitive word filtering, and asynchronous event notification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final CommentMapper commentMapper;
	private final SensitiveWordService sensitiveWordService;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	@LogOperation("Publish Comment")
	public ApiResponse<Void> publishComment(CommentRequest request, HttpServletRequest servletRequest) {
		// 1. Validate Target Post context
		Post targetPost = null;
		if (request.postId() != null) {
			targetPost = postRepository.findById(request.postId())
					.orElseThrow(() -> new ResourceNotFoundException("Post", "id", request.postId()));

			if (targetPost.getStatus() != PostStatus.PUBLISHED) {
				throw new BusinessException(BusinessCode.FORBIDDEN, "Cannot comment on unpublished posts");
			}
		}

		// 2. Content Moderation
		boolean hasViolation = sensitiveWordService.containsSensitiveWord(request.content());
		String filteredContent = sensitiveWordService.filter(request.content());

		// 3. Hierarchy Validation
		Comment parentComment = null;
		if (request.parentId() != null) {
			parentComment = commentRepository.findById(request.parentId())
					.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.parentId()));

			boolean contextMatch = (targetPost == null && parentComment.getPost() == null) || (targetPost != null
					&& parentComment.getPost() != null && parentComment.getPost().getId().equals(targetPost.getId()));

			if (!contextMatch) {
				throw new BusinessException(BusinessCode.BAD_REQUEST, "Comment context mismatch with parent");
			}
		}

		User author = SecurityUtil.getCurrentUserOrThrow(userRepository);

		// 4. Persistence
		var comment = new Comment();
		comment.setContent(filteredContent);
		comment.setPost(targetPost);
		comment.setUser(author);
		comment.setParent(parentComment);
		comment.setIpAddress(IpUtil.getIpAddress(servletRequest));
		comment.setUserAgent(servletRequest.getHeader("User-Agent"));

		if (hasViolation) {
			comment.setStatus(CommentStatus.REJECTED);
			log.warn("Comment by {} automatically rejected due to policy violation", author.getUsername());
		} else {
			comment.setStatus(CommentStatus.PENDING);
		}

		commentRepository.save(comment);
		eventPublisher.publishEvent(new CommentSubmittedEvent(this, comment));

		if (hasViolation) {
			return ApiResponse.error(BusinessCode.BAD_REQUEST, "Content policy violation detected. Comment rejected.");
		}

		return ApiResponse.success("Comment submitted successfully and is awaiting moderation", null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> retrieveCommentsByPost(Long postId, Pageable pageable) {
		var comments = commentRepository.findAllByPostIdAndParentIsNullAndStatus(postId, CommentStatus.APPROVED,
				pageable);
		return ApiResponse.success(PageResult.of(comments.map(commentMapper::toResponse)));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> retrieveGuestbookComments(Pageable pageable) {
		var comments = commentRepository.findAllByPostIsNullAndParentIsNullAndStatus(CommentStatus.APPROVED, pageable);
		return ApiResponse.success(PageResult.of(comments.map(commentMapper::toResponse)));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> searchCommentsForManagement(Pageable pageable) {
		var comments = commentRepository.findAll(pageable);
		return ApiResponse.success(PageResult.of(comments.map(commentMapper::toResponse)));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CommentResponse>> retrievePendingComments(Pageable pageable) {
		var comments = commentRepository.findAllByStatus(CommentStatus.PENDING, pageable);
		return ApiResponse.success(PageResult.of(comments.map(commentMapper::toResponse)));
	}

	@Override
	@Transactional
	@LogOperation("Moderate Comment")
	public ApiResponse<Void> moderateComment(Long id, CommentStatus status) {
		var comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

		comment.setStatus(status);
		commentRepository.save(comment);
		log.info("Comment {} moderation status updated to {}", id, status);
		return ApiResponse.success("Moderation successful", null);
	}

	@Override
	@Transactional
	@LogOperation("Delete Comment")
	public ApiResponse<Void> deleteComment(Long id) {
		if (!commentRepository.existsById(id)) {
			throw new ResourceNotFoundException("Comment", "id", id);
		}
		commentRepository.deleteById(id);
		log.info("Comment {} permanently deleted", id);
		return ApiResponse.success("Comment deleted", null);
	}
}
