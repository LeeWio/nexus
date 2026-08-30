package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.response.ContentWorkflowResponse;
import space.nebula.nexus.payload.response.ContentWorkflowResponse.Item;
import space.nebula.nexus.payload.response.ContentWorkflowResponse.Priority;
import space.nebula.nexus.payload.response.ContentWorkflowResponse.Summary;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.service.IContentWorkflowService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentWorkflowServiceImpl implements IContentWorkflowService {

	private static final int ITEMS_PER_STATUS = 6;

	private final PostRepository postRepository;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<ContentWorkflowResponse> getWorkflow() {
		List<Item> items = new ArrayList<>();
		addItems(items, PostStatus.PENDING_REVIEW);
		addItems(items, PostStatus.REJECTED);
		addItems(items, PostStatus.SCHEDULED);
		addItems(items, PostStatus.DRAFT);
		items.sort(Comparator.comparing(Item::priority).thenComparing(Item::relevantAt,
				Comparator.nullsLast(Comparator.reverseOrder())));

		long needsReview = postRepository.countByStatus(PostStatus.PENDING_REVIEW);
		long scheduled = postRepository.countByStatus(PostStatus.SCHEDULED);
		long drafts = postRepository.countByStatus(PostStatus.DRAFT);
		long rejected = postRepository.countByStatus(PostStatus.REJECTED);
		Summary summary = new Summary(needsReview, scheduled, drafts, rejected,
				needsReview + scheduled + drafts + rejected);
		return ApiResponse.success("Content workflow retrieved successfully",
				new ContentWorkflowResponse(items, summary));
	}

	private void addItems(List<Item> items, PostStatus status) {
		PageRequest pageRequest = PageRequest.of(0, ITEMS_PER_STATUS, Sort.by(Sort.Direction.DESC, "updatedAt"));
		postRepository.findAllByStatus(status, pageRequest).getContent().stream().map(post -> toItem(post, status))
				.forEach(items::add);
	}

	private Item toItem(Post post, PostStatus status) {
		return new Item("post-" + post.getId(), "POST", post.getTitle(), description(post, status), action(status),
				priority(status), status, relevantAt(post, status), "/posts?id=" + post.getId());
	}

	private String description(Post post, PostStatus status) {
		String summary = post.getSummary();
		if (summary == null || summary.isBlank()) {
			summary = post.getAutoSummary();
		}
		if (summary != null && !summary.isBlank()) {
			return summary;
		}
		return switch (status) {
			case PENDING_REVIEW -> "A publishing decision is waiting.";
			case REJECTED -> "Resolve the review note before resubmitting.";
			case SCHEDULED -> "Confirm the release details before publication.";
			case DRAFT -> "Continue shaping this piece when you have a moment.";
			default -> "Review this content item.";
		};
	}

	private String action(PostStatus status) {
		return switch (status) {
			case PENDING_REVIEW -> "Review";
			case REJECTED -> "Resolve";
			case SCHEDULED -> "Check release";
			case DRAFT -> "Continue";
			default -> "Open";
		};
	}

	private Priority priority(PostStatus status) {
		return switch (status) {
			case PENDING_REVIEW, REJECTED -> Priority.HIGH;
			case SCHEDULED -> Priority.MEDIUM;
			case DRAFT -> Priority.LOW;
			default -> Priority.LOW;
		};
	}

	private LocalDateTime relevantAt(Post post, PostStatus status) {
		return status == PostStatus.SCHEDULED ? post.getScheduledAt() : post.getUpdatedAt();
	}
}
