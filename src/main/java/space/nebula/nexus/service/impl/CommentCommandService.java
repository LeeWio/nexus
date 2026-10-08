package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.config.CommentModerationProperties;
import space.nebula.nexus.config.CommentThreadProperties;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentReportStatus;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.MomentVisibility;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.request.CommentReportRequest;
import space.nebula.nexus.payload.request.CommentUpdateRequest;
import space.nebula.nexus.payload.request.MomentCommentRequest;
import space.nebula.nexus.payload.response.CommentPublishResponse;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.SensitiveWordService;
import space.nebula.nexus.utils.IpUtil;

import cn.hutool.core.util.StrUtil;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentCommandService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final MomentRepository momentRepository;
	private final UserRepository userRepository;
	private final SensitiveWordService sensitiveWordService;
	private final ApplicationEventPublisher eventPublisher;
	private final JdbcTemplate jdbcTemplate;
	private final CommentGovernanceService governanceService;
	private final CommentModerationProperties moderationProperties;
	private final CommentThreadProperties threadProperties;
	private final CommentIdempotencyService idempotencyService;
	private final CommentMetricsService metricsService;
	private final GuestCommentIdentityService guestIdentityService;

	@Transactional
	@LogOperation("Publish Comment")
	public ApiResponse<CommentPublishResponse> publishComment(CommentRequest request,
			HttpServletRequest servletRequest) {
		Assert.isFalse(request.postId() != null && request.momentId() != null,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Provide either postId or momentId, not both"));
		Post targetPost = resolveTargetPost(request.postId());
		Moment targetMoment = resolveTargetMoment(request.momentId());
		String filteredContent = sensitiveWordService.filter(request.content());
		boolean hasViolation = request.content() != null && !request.content().equals(filteredContent);
		Comment parentComment = resolveParentComment(request.parentId(), targetPost, targetMoment);
		User author = SecurityUtil.getCurrentUser(userRepository);
		String guestTokenHash = null;
		String guestName = null;
		String guestEmail = null;
		if (author == null) {
			guestIdentityService.validateGuestName(request.guestName());
			guestName = request.guestName().trim();
			guestEmail = guestIdentityService.normalizeGuestEmail(request.guestEmail());
			guestTokenHash = guestIdentityService.claimTokenHash(servletRequest);
		}

		String clientRequestId = normalizeClientRequestId(servletRequest);
		String requestHash = idempotencyService.hashSubmission(request.postId(), request.momentId(), request.parentId(),
				filteredContent);
		Long authorId = author == null ? null : author.getId();
		var replayedResponse = idempotencyService.begin(authorId, guestTokenHash, clientRequestId, requestHash);
		if (replayedResponse.isPresent()) {
			return restoreReplayResponse(replayedResponse.get(), authorId, guestTokenHash, clientRequestId);
		}
		if (clientRequestId != null) {
			var existingComment = findExistingSubmission(authorId, guestTokenHash, clientRequestId);
			if (existingComment.isPresent()) {
				Assert.isTrue(
						isSameSubmission(existingComment.get(), targetPost, targetMoment, parentComment, filteredContent),
						() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
								"Idempotency-Key was already used for a different comment"));
				return ApiResponse.success("Comment submission already received.",
						new CommentPublishResponse(existingComment.get().getId(), existingComment.get().getStatus()));
			}
		}

		try {
			var comment = new Comment();
			comment.setContent(filteredContent);
			comment.setPost(targetPost);
			comment.setMoment(targetMoment);
			comment.setUser(author);
			comment.setGuestName(guestName);
			comment.setGuestEmail(guestEmail);
			comment.setGuestTokenHash(guestTokenHash);
			comment.setParent(parentComment);
			comment.setIpAddress(IpUtil.getIpAddress(servletRequest));
			comment.setUserAgent(servletRequest.getHeader("User-Agent"));
			comment.setClientRequestId(clientRequestId);
			boolean isAdmin = author != null && SecurityUtil.hasRole("ADMIN");
			if (isAdmin) {
				comment.setStatus(CommentStatus.APPROVED);
			} else {
				comment.setStatus(hasViolation ? CommentStatus.SPAM : CommentStatus.PENDING);
			}

			if (hasViolation && !isAdmin) {
				log.warn("Comment by {} automatically marked as spam due to policy violation",
						author == null ? guestName : author.getUsername());
			}

			commentRepository.saveAndFlush(comment);
			comment.updatePath(parentComment);
			commentRepository.save(comment);
			eventPublisher.publishEvent(buildSubmittedEvent(comment));
			metricsService.incrementPublished(comment.getStatus());

			ApiResponse<CommentPublishResponse> response;
			CommentPublishResponse result = new CommentPublishResponse(comment.getId(), comment.getStatus());
			if (isAdmin) {
				response = ApiResponse.success("Comment published successfully.", result);
			} else {
				response = hasViolation
						? ApiResponse.success("Comment received and flagged for moderation.", result)
						: ApiResponse.success("Comment submitted successfully. It is awaiting moderation.", result);
			}
			idempotencyService.complete(authorId, guestTokenHash, clientRequestId, requestHash, response,
					comment.getId());
			return response;
		} catch (DataIntegrityViolationException ex) {
			return recoverIdempotentSubmission(authorId, guestTokenHash, clientRequestId, requestHash, ex);
		}
	}

	@Transactional
	@LogOperation("Withdraw My Comment")
	public ApiResponse<Void> withdrawMyComment(Long id) {
		return deleteMyComment(id);
	}

	@Transactional
	@LogOperation("Update My Comment")
	public ApiResponse<Void> updateMyComment(Long id, CommentUpdateRequest request) {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Comment comment = findOwnedComment(id, currentUser);
		Assert.isFalse(comment.isDeletedPlaceholder(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Deleted comments cannot be edited"));
		String filteredContent = sensitiveWordService.filter(request.content());
		boolean hasViolation = request.content() != null && !request.content().equals(filteredContent);

		comment.editContent(filteredContent);
		boolean isAdmin = SecurityUtil.hasRole("ADMIN");
		if (isAdmin) {
			comment.setStatus(CommentStatus.APPROVED);
		} else {
			comment.setStatus(hasViolation ? CommentStatus.SPAM : CommentStatus.PENDING);
		}
		commentRepository.save(comment);
		log.info("User {} edited comment {} and status is now {}", currentUser.getUsername(), id, comment.getStatus());

		if (isAdmin) {
			return ApiResponse.success("Comment updated successfully.", null);
		}
		if (hasViolation) {
			return ApiResponse.success("Comment updated and flagged for moderation.", null);
		}
		return ApiResponse.success("Comment updated and submitted for moderation.", null);
	}

	@Transactional
	@CacheEvict(value = CacheConstants.MOMENTS, allEntries = true)
	@LogOperation("Delete My Comment")
	public ApiResponse<Void> deleteMyComment(Long id) {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Comment comment = findOwnedComment(id, currentUser);
		if (comment.isDeletedPlaceholder()) {
			return ApiResponse.success("Comment was already deleted.", null);
		}
		if (commentRepository.existsByParentId(id)) {
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
	public ApiResponse<Void> reportComment(Long id, CommentReportRequest request) {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
		Assert.isTrue(comment.getStatus() == CommentStatus.APPROVED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Only visible comments can be reported"));
		Assert.isFalse(comment.isDeletedPlaceholder(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Deleted comments cannot be reported"));
		Assert.isFalse(comment.getUser() != null && comment.getUser().getId().equals(currentUser.getId()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "You cannot report your own comment"));

		int inserted = jdbcTemplate.update(
				"INSERT IGNORE INTO blog_comment_report(comment_id, reporter_id, reason, description, status, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
				id, currentUser.getId(), request.reason(), request.description(), CommentReportStatus.OPEN.name());
		if (inserted > 0) {
			commentRepository.incrementReports(id, 1L);
			autoFlagReportedComment(comment);
		}
		metricsService.incrementReport(inserted > 0);
		return ApiResponse.success("Comment report received.", null);
	}

	private void autoFlagReportedComment(Comment comment) {
		if (comment.getStatus() != CommentStatus.APPROVED || governanceService
				.countOpenReports(comment.getId()) < moderationProperties.getAutoReviewReportThreshold()) {
			return;
		}
		CommentStatus previousStatus = comment.getStatus();
		comment.setStatus(CommentStatus.PENDING);
		commentRepository.save(comment);
		governanceService.recordModeration(comment, previousStatus, comment.getStatus(),
				CommentModerationAction.AUTO_FLAGGED, "REPORT_THRESHOLD",
				"Comment moved back to moderation after repeated reports.", null);
	}

	private Post resolveTargetPost(Long postId) {
		if (postId == null) {
			return null;
		}
		Post targetPost = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
		Assert.isTrue(targetPost.getStatus() == PostStatus.PUBLISHED,
				() -> new BusinessException(BusinessCode.FORBIDDEN, "Comments are disabled for unpublished posts"));
		return targetPost;
	}

	private Moment resolveTargetMoment(Long momentId) {
		if (momentId == null) {
			return null;
		}
		Moment targetMoment = momentRepository.findById(momentId)
				.orElseThrow(() -> new ResourceNotFoundException("Moment", "id", momentId));
		Assert.isTrue(targetMoment.getVisibility() == MomentVisibility.PUBLIC,
				() -> new BusinessException(BusinessCode.FORBIDDEN, "Comments are only available on public moments"));
		return targetMoment;
	}

	private Comment resolveParentComment(Long parentId, Post targetPost, Moment targetMoment) {
		if (parentId == null) {
			return null;
		}
		Comment parentComment = commentRepository.findById(parentId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", parentId));
		boolean contextMatch;
		if (targetMoment != null) {
			contextMatch = parentComment.getMoment() != null
					&& parentComment.getMoment().getId().equals(targetMoment.getId());
		} else if (targetPost != null) {
			contextMatch = parentComment.getPost() != null && parentComment.getPost().getId().equals(targetPost.getId());
		} else {
			contextMatch = parentComment.getPost() == null && parentComment.getMoment() == null;
		}
		Assert.isTrue(contextMatch,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Comment context does not match the parent"));
		Assert.isTrue(parentComment.getStatus() == CommentStatus.APPROVED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Replies can only be added to approved comments"));
		Assert.isFalse(parentComment.isDeletedPlaceholder(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Replies cannot target a deleted comment"));
		Assert.isTrue(replyDepth(parentComment) <= threadProperties.getMaxReplyDepth(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Replies can be nested up to " + threadProperties.getMaxReplyDepth() + " levels"));
		return parentComment;
	}

	private int replyDepth(Comment parentComment) {
		if (StrUtil.isNotBlank(parentComment.getPath())) {
			return StrUtil.splitTrim(parentComment.getPath(), '/').size();
		}

		int depth = 1;
		Comment cursor = parentComment;
		while (cursor.getParent() != null && depth <= threadProperties.getMaxReplyDepth()) {
			depth++;
			cursor = cursor.getParent();
		}
		return depth;
	}

	private String normalizeClientRequestId(HttpServletRequest servletRequest) {
		String value = servletRequest.getHeader("Idempotency-Key");
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		Assert.isTrue(normalized.length() <= 80,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Idempotency-Key must not exceed 80 characters"));
		return normalized;
	}

	private ApiResponse<CommentPublishResponse> recoverIdempotentSubmission(Long userId, String guestTokenHash,
			String clientRequestId, String requestHash, DataIntegrityViolationException ex) {
		if (clientRequestId == null) {
			throw ex;
		}
		return idempotencyService.findCompletedCommentId(userId, guestTokenHash, clientRequestId, requestHash)
				.flatMap(commentRepository::findById)
				.map(comment -> ApiResponse.success("Comment submission already received.",
						new CommentPublishResponse(comment.getId(), comment.getStatus())))
				.orElseThrow(() -> ex);
	}

	private ApiResponse<CommentPublishResponse> restoreReplayResponse(ApiResponse<Void> replayedResponse, Long userId,
			String guestTokenHash, String clientRequestId) {
		if (clientRequestId == null) {
			return ApiResponse.success(replayedResponse.message(), null);
		}
		return findExistingSubmission(userId, guestTokenHash, clientRequestId)
				.map(comment -> ApiResponse.success(replayedResponse.message(),
						new CommentPublishResponse(comment.getId(), comment.getStatus())))
				.orElseGet(() -> ApiResponse.success(replayedResponse.message(), null));
	}

	private java.util.Optional<Comment> findExistingSubmission(Long userId, String guestTokenHash,
			String clientRequestId) {
		if (userId != null) {
			return commentRepository.findByUserIdAndClientRequestId(userId, clientRequestId);
		}
		return commentRepository.findByGuestTokenHashAndClientRequestId(guestTokenHash, clientRequestId);
	}

	private boolean isSameSubmission(Comment existingComment, Post targetPost, Moment targetMoment,
			Comment parentComment, String filteredContent) {
		Long existingPostId = existingComment.getPost() == null ? null : existingComment.getPost().getId();
		Long targetPostId = targetPost == null ? null : targetPost.getId();
		Long existingMomentId = existingComment.getMoment() == null ? null : existingComment.getMoment().getId();
		Long targetMomentId = targetMoment == null ? null : targetMoment.getId();
		Long existingParentId = existingComment.getParent() == null ? null : existingComment.getParent().getId();
		Long targetParentId = parentComment == null ? null : parentComment.getId();
		return Objects.equals(existingPostId, targetPostId) && Objects.equals(existingMomentId, targetMomentId)
				&& Objects.equals(existingParentId, targetParentId)
				&& Objects.equals(existingComment.getContent(), filteredContent);
	}

	private CommentSubmittedEvent buildSubmittedEvent(Comment comment) {
		User author = comment.getUser();
		Post post = comment.getPost();
		Moment moment = comment.getMoment();
		String authorUsername = author == null ? "guest" : author.getUsername();
		String authorDisplayName = author == null ? comment.getGuestName()
				: (author.getNickname() != null ? author.getNickname() : author.getUsername());
		String postAuthorEmail = null;
		String postAuthorDisplayName = null;
		String postTitle = "Guestbook";

		if (post != null) {
			User postAuthor = post.getAuthor();
			postTitle = post.getTitle();
			if (postAuthor != null) {
				postAuthorEmail = postAuthor.getEmail();
				postAuthorDisplayName = postAuthor.getNickname() != null
						? postAuthor.getNickname()
						: postAuthor.getUsername();
			}
		} else if (moment != null) {
			postTitle = "Moment #" + moment.getId();
			User momentAuthor = moment.getUser();
			if (momentAuthor != null) {
				postAuthorEmail = momentAuthor.getEmail();
				postAuthorDisplayName = momentAuthor.getNickname() != null
						? momentAuthor.getNickname()
						: momentAuthor.getUsername();
			}
		}

		return new CommentSubmittedEvent(this, comment.getId(), authorUsername, authorDisplayName,
				comment.getContent(), comment.getStatus(), postTitle, postAuthorEmail, postAuthorDisplayName,
				comment.getIpAddress(), comment.getUserAgent());
	}

	@Transactional
	@CacheEvict(value = CacheConstants.MOMENTS, allEntries = true)
	@LogOperation("Publish Moment Comment")
	public ApiResponse<CommentPublishResponse> publishMomentComment(MomentCommentRequest request,
			HttpServletRequest servletRequest) {
		return publishComment(new CommentRequest(request.content(), null, request.momentId(), request.parentId(),
				request.guestName(), request.guestEmail()), servletRequest);
	}

	private Comment findOwnedComment(Long id, User currentUser) {
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
		boolean ownsComment = comment.getUser() != null && comment.getUser().getId().equals(currentUser.getId());
		Assert.isTrue(ownsComment,
				() -> new BusinessException(BusinessCode.FORBIDDEN, "You can only manage your own comments"));
		return comment;
	}
}
