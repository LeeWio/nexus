package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostRevisionKind;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.PostArchiveRequest;
import space.nebula.nexus.payload.request.PostScheduleRequest;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.IPostPublicationService;
import space.nebula.nexus.service.IPostRevisionService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostPublicationService implements IPostPublicationService {

	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final IPostRevisionService postRevisionService;

	@Override
	@Transactional
	@LogOperation("Submit Post For Review")
	public ApiResponse<Void> submitForReview(Long id) {
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.DRAFT || post.getStatus() == PostStatus.REJECTED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Only Draft or Rejected posts can be submitted for review"));

		post.setStatus(PostStatus.PENDING_REVIEW);
		post.setReviewComment(null);
		post.setReviewedAt(null);
		post.setReviewedBy(null);
		postRepository.save(post);
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.SUBMITTED_FOR_REVIEW));
		log.info("Post '{}' submitted for review by {}", post.getTitle(),
				SecurityUtil.getCurrentUserOrThrow(userRepository).getUsername());
		return ApiResponse.success("Post submitted for review successfully", null);
	}

	@Override
	@Transactional
	@LogOperation("Withdraw Post From Review")
	public ApiResponse<Void> withdrawFromReview(Long id) {
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.PENDING_REVIEW,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post is not pending review"));

		post.withdrawFromReview();
		postRepository.save(post);
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.WITHDRAWN_FROM_REVIEW));
		log.info("Post '{}' withdrawn from review", post.getTitle());
		return ApiResponse.success("Post withdrawn from review", null);
	}

	@Override
	@Transactional
	@LogOperation("Review Post")
	@CacheEvict(value = {CacheConstants.BLOG_POSTS, CacheConstants.SEO}, allEntries = true)
	public ApiResponse<Void> reviewPost(Long id, boolean approved, String reviewComment) {
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.PENDING_REVIEW,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post is not pending review"));
		if (!approved) {
			Assert.notBlank(reviewComment,
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "A rejection reason is required"));
		}
		User reviewer = SecurityUtil.getCurrentUserOrThrow(userRepository);
		post.setReviewComment(StrUtil.blankToDefault(reviewComment, null));
		post.setReviewedAt(LocalDateTime.now());
		post.setReviewedBy(reviewer);

		if (approved) {
			post.publish();
			log.info("Post '{}' approved and published", post.getTitle());
			eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.PUBLISHED));
		} else {
			post.reject();
			log.info("Post '{}' rejected. Reason: {}", post.getTitle(), reviewComment);
		}

		postRepository.save(post);
		if (!approved) {
			eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.REJECTED));
		}
		return ApiResponse.success(approved ? "Post approved and published" : "Post rejected", null);
	}

	@Override
	@Transactional
	@LogOperation("Schedule Post Publication")
	public ApiResponse<Void> schedulePost(Long id, PostScheduleRequest request) {
		Assert.notNull(request,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "A publication schedule is required"));
		Assert.notNull(request.scheduledAt(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "A publication time is required"));
		Assert.isTrue(request.scheduledAt().isAfter(LocalDateTime.now()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "The publication time must be in the future"));
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.PENDING_REVIEW || post.getStatus() == PostStatus.SCHEDULED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Only posts awaiting review or already scheduled can be scheduled"));

		User reviewer = SecurityUtil.getCurrentUserOrThrow(userRepository);
		post.schedule(request.scheduledAt());
		post.setReviewComment(null);
		post.setReviewedAt(LocalDateTime.now());
		post.setReviewedBy(reviewer);
		postRepository.save(post);
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.SCHEDULED));
		log.info("Post '{}' scheduled for publication at {} by {}", post.getTitle(), request.scheduledAt(),
				reviewer.getUsername());
		return ApiResponse.success("Post publication scheduled", null);
	}

	@Override
	@Transactional
	@LogOperation("Cancel Scheduled Post Publication")
	public ApiResponse<Void> cancelScheduledPost(Long id) {
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.SCHEDULED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post is not scheduled for publication"));

		post.cancelSchedule();
		post.setReviewComment(null);
		post.setReviewedAt(null);
		post.setReviewedBy(null);
		postRepository.save(post);
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.SCHEDULE_CANCELED));
		log.info("Scheduled publication canceled for post '{}'", post.getTitle());
		return ApiResponse.success("Scheduled publication canceled", null);
	}

	@Override
	@Transactional
	@LogOperation("Archive Published Post")
	public ApiResponse<Void> archivePost(Long id, PostArchiveRequest request) {
		Assert.notNull(request,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "An archive request is required"));
		Assert.notBlank(request.reason(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "An archive reason is required"));
		Assert.isTrue(request.reason().trim().length() <= 1000, () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"The archive reason must not exceed 1000 characters"));

		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.PUBLISHED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Only published posts can be archived"));
		User editor = SecurityUtil.getCurrentUserOrThrow(userRepository);
		post.archive(request.reason().trim(), editor);
		postRepository.save(post);
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.ARCHIVED));
		log.info("Post '{}' archived by {}", post.getTitle(), editor.getUsername());
		return ApiResponse.success("Post archived", null);
	}

	@Override
	@Transactional
	@LogOperation("Restore Archived Post")
	public ApiResponse<Void> restoreArchivedPost(Long id) {
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.ARCHIVED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post is not archived"));

		post.restoreToDraft();
		postRepository.save(post);
		postRevisionService.saveRevision(post, PostRevisionKind.RESTORED,
				"Archived post restored for a new editing cycle");
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.RESTORED_TO_DRAFT));
		log.info("Archived post '{}' restored to draft", post.getTitle());
		return ApiResponse.success("Post restored to draft", null);
	}

	@Override
	@Transactional
	public int publishDueScheduledPosts(LocalDateTime now, int batchSize) {
		Assert.notNull(now, () -> new BusinessException(BusinessCode.BAD_REQUEST, "Publication cutoff is required"));
		Assert.isTrue(batchSize >= 1 && batchSize <= 100, () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"Publication batch size must be between 1 and 100"));

		List<Long> duePostIds = postRepository.findDueScheduledPostIds(PostStatus.SCHEDULED, now,
				PageRequest.of(0, batchSize));
		if (duePostIds.isEmpty()) {
			return 0;
		}
		List<Post> duePosts = postRepository.findScheduledPublicationBatch(PostStatus.SCHEDULED, duePostIds).stream()
				.sorted(Comparator.comparing(Post::getScheduledAt).thenComparing(Post::getId)).toList();
		for (Post post : duePosts) {
			post.publish();
		}
		postRepository.saveAll(duePosts);
		for (Post post : duePosts) {
			eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.PUBLISHED));
		}
		if (!duePosts.isEmpty()) {
			log.info("Published {} scheduled posts at {}", duePosts.size(), now);
		}
		return duePosts.size();
	}

	private Post findPostForUpdateOrThrow(Long id) {
		return postRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
	}
}
