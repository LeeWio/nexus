package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.event.CommentModeratedEvent;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentReportStatus;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.repository.CommentRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentModerationService {

	private final CommentRepository commentRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final CommentGovernanceService governanceService;
	private final CommentMetricsService metricsService;

	@Transactional
	@LogOperation("Moderate Comment")
	public ApiResponse<Void> moderateComment(Long id, CommentStatus status) {
		validateModerationStatus(status);
		var comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
		Assert.isFalse(comment.isDeletedPlaceholder(), () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"Deleted comment placeholders cannot be moderated"));
		CommentStatus previousStatus = comment.getStatus();

		if (!applyModerationStatus(comment, status)) {
			return ApiResponse.success("Comment moderation status is already up to date", null);
		}

		commentRepository.save(comment);
		governanceService.recordModeration(comment, previousStatus, comment.getStatus(),
				CommentModerationAction.STATUS_CHANGED, "MANUAL_MODERATION", null, null);
		resolveReportsForStatus(comment.getId(), status);
		publishModerationEvent(comment, status);
		metricsService.incrementModeration(CommentModerationAction.STATUS_CHANGED, status);
		log.info("Comment {} moderation status updated to {}", id, status);
		return ApiResponse.success("Moderation completed successfully.", null);
	}

	@Transactional
	@LogOperation("Batch Moderate Comments")
	public ApiResponse<Integer> batchModerateComments(List<Long> ids, CommentStatus status) {
		Assert.notEmpty(ids, () -> new BusinessException(BusinessCode.BAD_REQUEST, "Comment IDs are required"));
		validateModerationStatus(status);
		int updated = 0;
		String batchId = UUID.randomUUID().toString();
		for (Long id : ids.stream().distinct().toList()) {
			Comment comment = commentRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
			Assert.isFalse(comment.isDeletedPlaceholder(), () -> new BusinessException(BusinessCode.BAD_REQUEST,
					"Deleted comment placeholders cannot be moderated"));
			CommentStatus previousStatus = comment.getStatus();
			if (applyModerationStatus(comment, status)) {
				commentRepository.save(comment);
				governanceService.recordModeration(comment, previousStatus, comment.getStatus(),
						CommentModerationAction.STATUS_CHANGED, "BATCH_MODERATION", null, batchId);
				resolveReportsForStatus(comment.getId(), status);
				publishModerationEvent(comment, status);
				metricsService.incrementModeration(CommentModerationAction.STATUS_CHANGED, status);
				updated++;
			}
		}
		log.info("Batch moderation updated {} comments to {}", updated, status);
		return ApiResponse.success("Batch moderation completed successfully.", updated);
	}

	@Transactional
	public ApiResponse<Void> pinComment(Long id, boolean pinned) {
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
		Assert.isFalse(pinned && comment.isDeletedPlaceholder(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Deleted comment placeholders cannot be pinned"));
		comment.setPinned(pinned);
		commentRepository.save(comment);
		return ApiResponse.success(pinned ? "Comment pinned." : "Comment unpinned.", null);
	}

	@Transactional
	public ApiResponse<Void> featureComment(Long id, boolean featured) {
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
		Assert.isFalse(featured && comment.isDeletedPlaceholder(), () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"Deleted comment placeholders cannot be featured"));
		comment.setFeatured(featured);
		commentRepository.save(comment);
		return ApiResponse.success(featured ? "Comment featured." : "Comment unfeatured.", null);
	}

	@Transactional
	@LogOperation("Delete Comment")
	public ApiResponse<Void> deleteComment(Long id) {
		Assert.isTrue(commentRepository.existsById(id), () -> new ResourceNotFoundException("Comment", "id", id));

		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
		if (comment.isDeletedPlaceholder()) {
			return ApiResponse.success("Comment was already deleted.", null);
		}
		governanceService.recordModeration(comment, comment.getStatus(), comment.getStatus(),
				CommentModerationAction.DELETED, "ADMIN_DELETE", null, null);
		governanceService.resolveOpenReports(id, CommentReportStatus.ACTIONED, "Comment deleted by moderator.");
		metricsService.incrementModeration(CommentModerationAction.DELETED, comment.getStatus());
		if (commentRepository.existsByParentId(id)) {
			comment.markDeletedPlaceholder();
			commentRepository.save(comment);
			log.info("Comment {} converted to deleted placeholder to preserve thread continuity", id);
			return ApiResponse.success("Comment converted to deleted placeholder.", null);
		}
		commentRepository.deleteById(id);
		log.info("Comment {} archived through soft deletion", id);
		return ApiResponse.success("Comment deleted successfully.", null);
	}

	private void resolveReportsForStatus(Long commentId, CommentStatus status) {
		if (status == CommentStatus.APPROVED) {
			governanceService.resolveOpenReports(commentId, CommentReportStatus.DISMISSED,
					"Comment approved by moderator.");
		} else if (status == CommentStatus.REJECTED || status == CommentStatus.SPAM) {
			governanceService.resolveOpenReports(commentId, CommentReportStatus.ACTIONED,
					"Comment hidden by moderator.");
		}
	}

	private boolean applyModerationStatus(Comment comment, CommentStatus status) {
		if (comment.getStatus() == status) {
			return false;
		}

		if (status == CommentStatus.APPROVED) {
			Assert.isTrue(comment.getParent() == null || comment.getParent().getStatus() == CommentStatus.APPROVED,
					() -> new BusinessException(BusinessCode.BAD_REQUEST,
							"Approve the parent comment before approving this reply"));
			comment.approve();
		} else if (status == CommentStatus.REJECTED || status == CommentStatus.SPAM) {
			Assert.isFalse(commentRepository.existsByParentIdAndStatus(comment.getId(), CommentStatus.APPROVED),
					() -> new BusinessException(BusinessCode.BAD_REQUEST,
							"Reject or mark approved replies as spam before hiding their parent comment"));
			if (status == CommentStatus.SPAM) {
				comment.setStatus(CommentStatus.SPAM);
			} else {
				comment.reject();
			}
		} else {
			comment.setStatus(status);
		}
		return true;
	}

	private void validateModerationStatus(CommentStatus status) {
		Assert.isTrue(
				status == CommentStatus.APPROVED || status == CommentStatus.REJECTED || status == CommentStatus.SPAM,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Moderation status must be APPROVED, REJECTED, or SPAM"));
	}

	private void publishModerationEvent(Comment comment, CommentStatus status) {
		Long replyRecipientId = comment.getParent() == null ? null : comment.getParent().getUser().getId();
		var post = comment.getPost();
		Long postAuthorId = post == null || post.getAuthor() == null ? null : post.getAuthor().getId();
		String postTitle = post == null ? null : post.getTitle();
		String link = status == CommentStatus.APPROVED ? buildCommentLink(comment) : null;
		eventPublisher.publishEvent(new CommentModeratedEvent(this, comment.getId(), comment.getUser().getId(),
				replyRecipientId, postAuthorId, comment.getUser().getUsername(), postTitle, status, link));
	}

	private String buildCommentLink(Comment comment) {
		String anchor = "#comment-" + comment.getId();
		return comment.getPost() == null ? "/guestbook" + anchor : "/posts/" + comment.getPost().getSlug() + anchor;
	}
}
