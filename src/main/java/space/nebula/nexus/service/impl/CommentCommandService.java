package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.config.CommentModerationProperties;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentReportStatus;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.request.CommentReportRequest;
import space.nebula.nexus.payload.request.CommentUpdateRequest;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.SensitiveWordService;
import space.nebula.nexus.utils.IpUtil;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentCommandService
{

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final SensitiveWordService sensitiveWordService;
	private final ApplicationEventPublisher eventPublisher;
	private final JdbcTemplate jdbcTemplate;
	private final CommentGovernanceService governanceService;
	private final CommentModerationProperties moderationProperties;
	private final CommentIdempotencyService idempotencyService;
	private final CommentMetricsService metricsService;

	@Transactional
	@LogOperation("Publish Comment")
	public ApiResponse<Void> publishComment(CommentRequest request, HttpServletRequest servletRequest)
	{
		Post targetPost = resolveTargetPost(request.postId());
		String filteredContent = sensitiveWordService.filter(request.content());
		boolean hasViolation = request.content() != null && !request.content().equals(filteredContent);
		Comment parentComment = resolveParentComment(request.parentId(), targetPost);
		User author = SecurityUtil.getCurrentUserOrThrow(userRepository);

		String clientRequestId = normalizeClientRequestId(servletRequest);
		String requestHash = idempotencyService.hashSubmission(request.postId(), request.parentId(), filteredContent);
		var replayedResponse = idempotencyService.begin(author.getId(), clientRequestId, requestHash);
		if (replayedResponse.isPresent())
		{
			return replayedResponse.get();
		}
		if (clientRequestId != null)
		{
			var existingComment = commentRepository.findByUserIdAndClientRequestId(author.getId(), clientRequestId);
			if (existingComment.isPresent())
			{
				Assert.isTrue(isSameSubmission(existingComment.get(), targetPost, parentComment, filteredContent),
						() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
								"Idempotency-Key was already used for a different comment"));
				return ApiResponse.success("Comment submission already received.", null);
			}
		}

		try
		{
			var comment = new Comment();
			comment.setContent(filteredContent);
			comment.setPost(targetPost);
			comment.setUser(author);
			comment.setParent(parentComment);
			comment.setIpAddress(IpUtil.getIpAddress(servletRequest));
			comment.setUserAgent(servletRequest.getHeader("User-Agent"));
			comment.setClientRequestId(clientRequestId);
			comment.setStatus(hasViolation ? CommentStatus.SPAM : CommentStatus.PENDING);
			if (hasViolation)
			{
				log.warn("Comment by {} automatically marked as spam due to policy violation", author.getUsername());
			}

			commentRepository.saveAndFlush(comment);
			comment.updatePath(parentComment);
			commentRepository.save(comment);
			eventPublisher.publishEvent(buildSubmittedEvent(comment));
			metricsService.incrementPublished(comment.getStatus());

			ApiResponse<Void> response = hasViolation
					? ApiResponse.success("Comment received and flagged for moderation.", null)
					: ApiResponse.success("Comment submitted successfully. It is awaiting moderation.", null);
			idempotencyService.complete(author.getId(), clientRequestId, requestHash, response, comment.getId());
			return response;
		}
		catch (DataIntegrityViolationException ex)
		{
			return recoverIdempotentSubmission(author.getId(), clientRequestId, requestHash, ex);
		}
	}

	@Transactional
	@LogOperation("Withdraw My Comment")
	public ApiResponse<Void> withdrawMyComment(Long id)
	{
		return deleteMyComment(id);
	}

	@Transactional
	@LogOperation("Update My Comment")
	public ApiResponse<Void> updateMyComment(Long id, CommentUpdateRequest request)
	{
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Comment comment = findOwnedComment(id, currentUser);
		String filteredContent = sensitiveWordService.filter(request.content());
		boolean hasViolation = request.content() != null && !request.content().equals(filteredContent);

		comment.editContent(filteredContent);
		comment.setStatus(hasViolation ? CommentStatus.SPAM : CommentStatus.PENDING);
		commentRepository.save(comment);
		log.info("User {} edited comment {} and reset status to {}", currentUser.getUsername(), id,
				comment.getStatus());

		if (hasViolation)
		{
			return ApiResponse.success("Comment updated and flagged for moderation.", null);
		}
		return ApiResponse.success("Comment updated and submitted for moderation.", null);
	}

	@Transactional
	@LogOperation("Delete My Comment")
	public ApiResponse<Void> deleteMyComment(Long id)
	{
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Comment comment = findOwnedComment(id, currentUser);
		if (commentRepository.existsByParentId(id))
		{
			comment.markDeletedPlaceholder();
			commentRepository.save(comment);
			log.info("User {} converted comment {} to deleted placeholder", currentUser.getUsername(), id);
			return ApiResponse.success("Comment deleted and thread preserved.", null);
		}

		commentRepository.delete(comment);
		log.info("User {} deleted comment {}", currentUser.getUsername(), id);
		return ApiResponse.success("Comment deleted successfully.", null);
	}

	@Transactional
	@LogOperation("Report Comment")
	public ApiResponse<Void> reportComment(Long id, CommentReportRequest request)
	{
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
		Assert.isTrue(comment.getStatus() == CommentStatus.APPROVED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Only visible comments can be reported"));
		Assert.isFalse(comment.getUser().getId().equals(currentUser.getId()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "You cannot report your own comment"));

		int inserted = jdbcTemplate.update(
				"INSERT IGNORE INTO blog_comment_report(comment_id, reporter_id, reason, description, status, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
				id, currentUser.getId(), request.reason(), request.description(), CommentReportStatus.OPEN.name());
		if (inserted > 0)
		{
			commentRepository.incrementReports(id, 1L);
			autoFlagReportedComment(comment);
		}
		metricsService.incrementReport(inserted > 0);
		return ApiResponse.success("Comment report received.", null);
	}

	private void autoFlagReportedComment(Comment comment)
	{
		if (comment.getStatus() != CommentStatus.APPROVED
				|| governanceService.countOpenReports(comment.getId()) < moderationProperties.getAutoReviewReportThreshold())
		{
			return;
		}
		CommentStatus previousStatus = comment.getStatus();
		comment.setStatus(CommentStatus.PENDING);
		commentRepository.save(comment);
		governanceService.recordModeration(comment, previousStatus, comment.getStatus(),
				CommentModerationAction.AUTO_FLAGGED, "REPORT_THRESHOLD",
				"Comment moved back to moderation after repeated reports.", null);
	}

	private Post resolveTargetPost(Long postId)
	{
		if (postId == null)
		{
			return null;
		}
		Post targetPost = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
		Assert.isTrue(targetPost.getStatus() == PostStatus.PUBLISHED,
				() -> new BusinessException(BusinessCode.FORBIDDEN, "Comments are disabled for unpublished posts"));
		return targetPost;
	}

	private Comment resolveParentComment(Long parentId, Post targetPost)
	{
		if (parentId == null)
		{
			return null;
		}
		Comment parentComment = commentRepository.findById(parentId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", parentId));
		boolean contextMatch = (targetPost == null && parentComment.getPost() == null)
				|| (targetPost != null && parentComment.getPost() != null
						&& parentComment.getPost().getId().equals(targetPost.getId()));
		Assert.isTrue(contextMatch,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Comment context does not match the parent"));
		Assert.isTrue(parentComment.getStatus() == CommentStatus.APPROVED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Replies can only be added to approved comments"));
		return parentComment;
	}

	private String normalizeClientRequestId(HttpServletRequest servletRequest)
	{
		String value = servletRequest.getHeader("Idempotency-Key");
		if (value == null || value.isBlank())
		{
			return null;
		}
		String normalized = value.trim();
		Assert.isTrue(normalized.length() <= 80,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Idempotency-Key must not exceed 80 characters"));
		return normalized;
	}

	private ApiResponse<Void> recoverIdempotentSubmission(Long userId, String clientRequestId, String requestHash,
			DataIntegrityViolationException ex)
	{
		if (clientRequestId == null)
		{
			throw ex;
		}
		return idempotencyService.findCompletedCommentId(userId, clientRequestId, requestHash)
				.map(commentId -> ApiResponse.<Void>success("Comment submission already received.", null))
				.orElseThrow(() -> ex);
	}

	private boolean isSameSubmission(Comment existingComment, Post targetPost, Comment parentComment,
			String filteredContent)
	{
		Long existingPostId = existingComment.getPost() == null ? null : existingComment.getPost().getId();
		Long targetPostId = targetPost == null ? null : targetPost.getId();
		Long existingParentId = existingComment.getParent() == null ? null : existingComment.getParent().getId();
		Long targetParentId = parentComment == null ? null : parentComment.getId();
		return Objects.equals(existingPostId, targetPostId) && Objects.equals(existingParentId, targetParentId)
				&& Objects.equals(existingComment.getContent(), filteredContent);
	}

	private CommentSubmittedEvent buildSubmittedEvent(Comment comment)
	{
		User author = comment.getUser();
		Post post = comment.getPost();
		String authorDisplayName = author.getNickname() != null ? author.getNickname() : author.getUsername();
		String postAuthorEmail = null;
		String postAuthorDisplayName = null;
		String postTitle = "Guestbook";

		if (post != null)
		{
			User postAuthor = post.getAuthor();
			postTitle = post.getTitle();
			if (postAuthor != null)
			{
				postAuthorEmail = postAuthor.getEmail();
				postAuthorDisplayName = postAuthor.getNickname() != null ? postAuthor.getNickname()
						: postAuthor.getUsername();
			}
		}

		return new CommentSubmittedEvent(this, comment.getId(), author.getUsername(), authorDisplayName,
				comment.getContent(), comment.getStatus(), postTitle, postAuthorEmail, postAuthorDisplayName,
				comment.getIpAddress(), comment.getUserAgent());
	}

	private Comment findOwnedComment(Long id, User currentUser)
	{
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
		Assert.isTrue(comment.getUser().getId().equals(currentUser.getId()),
				() -> new BusinessException(BusinessCode.FORBIDDEN, "You can only manage your own comments"));
		return comment;
	}
}
