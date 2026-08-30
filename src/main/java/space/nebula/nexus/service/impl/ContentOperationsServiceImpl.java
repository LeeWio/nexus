package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.response.ContentOperationsOverviewResponse;
import space.nebula.nexus.payload.response.ContentOperationsOverviewResponse.ActivityItem;
import space.nebula.nexus.payload.response.ContentOperationsOverviewResponse.AttentionItem;
import space.nebula.nexus.payload.response.ContentOperationsOverviewResponse.AttentionSeverity;
import space.nebula.nexus.payload.response.ContentOperationsOverviewResponse.QueueItem;
import space.nebula.nexus.payload.response.ContentOperationsOverviewResponse.Summary;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.NotificationRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.SubscriberRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.enums.SubscriberStatus;
import space.nebula.nexus.service.IContentOperationsService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentOperationsServiceImpl implements IContentOperationsService {

	private static final int QUEUE_LIMIT = 8;
	private static final int ACTIVITY_LIMIT = 8;

	private final PostRepository postRepository;
	private final MomentRepository momentRepository;
	private final CommentRepository commentRepository;
	private final NotificationRepository notificationRepository;
	private final SubscriberRepository subscriberRepository;
	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<ContentOperationsOverviewResponse> getOverview() {
		long pendingComments = commentRepository.countByStatus(CommentStatus.PENDING);
		long unreadNotifications = notificationRepository.countByRecipientIdAndIsVisibleTrueAndIsReadFalse(
				SecurityUtil.getCurrentUserOrThrow(userRepository).getId());

		Summary summary = new Summary(postRepository.countByStatus(PostStatus.PUBLISHED),
				postRepository.countByStatus(PostStatus.DRAFT), postRepository.countByStatus(PostStatus.PENDING_REVIEW),
				postRepository.countByStatus(PostStatus.SCHEDULED), momentRepository.count(), pendingComments,
				unreadNotifications, subscriberRepository.countByStatus(SubscriberStatus.ACTIVE));

		List<Post> recentPosts = postRepository.findTop10ByOrderByUpdatedAtDesc();
		List<Moment> recentMoments = momentRepository.findTop10ByOrderByCreatedAtDesc();

		List<QueueItem> editorialQueue = recentPosts.stream().limit(QUEUE_LIMIT).map(this::toQueueItem).toList();
		List<ActivityItem> recentActivity = new ArrayList<>();
		recentPosts.stream().map(this::toPostActivity).forEach(recentActivity::add);
		recentMoments.stream().map(this::toMomentActivity).forEach(recentActivity::add);
		recentActivity.sort(Comparator.comparing(ActivityItem::occurredAt,
				Comparator.nullsLast(Comparator.reverseOrder())));

		List<AttentionItem> attentionItems = new ArrayList<>();
		addAttention(attentionItems, "pending-comments", "Comments", "Comments are waiting for moderation.",
				"/comments", pendingComments, AttentionSeverity.WARNING);
		addAttention(attentionItems, "unread-notifications", "Notifications", "Unread workspace events need a look.",
				"/notifications", unreadNotifications, AttentionSeverity.INFO);
		addAttention(attentionItems, "pending-review", "Editorial review", "Posts are waiting for a publishing decision.",
				"/posts?status=PENDING_REVIEW", summary.pendingReview(), AttentionSeverity.WARNING);

		ContentOperationsOverviewResponse overview = new ContentOperationsOverviewResponse(summary, attentionItems,
				editorialQueue, recentActivity.stream().limit(ACTIVITY_LIMIT).toList(), LocalDateTime.now());
		return ApiResponse.success("Content operations overview retrieved successfully", overview);
	}

	private QueueItem toQueueItem(Post post) {
		return new QueueItem("post-" + post.getId(), "POST", post.getTitle(), firstNonBlank(post.getSummary(),
				post.getAutoSummary()), post.getStatus(), post.getUpdatedAt(), "/posts?id=" + post.getId());
	}

	private ActivityItem toPostActivity(Post post) {
		return new ActivityItem("post-" + post.getId(), "POST", post.getTitle(), post.getUpdatedAt(),
				"/posts?id=" + post.getId());
	}

	private ActivityItem toMomentActivity(Moment moment) {
		return new ActivityItem("moment-" + moment.getId(), "MOMENT", excerpt(moment.getContent()),
				moment.getCreatedAt(), "/moments?id=" + moment.getId());
	}

	private void addAttention(List<AttentionItem> items, String id, String title, String description, String href,
			long count, AttentionSeverity severity) {
		if (count > 0) {
			items.add(new AttentionItem(id, "WORKFLOW", title, description, href, severity, count));
		}
	}

	private String firstNonBlank(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private String excerpt(String value) {
		if (value == null) {
			return "Untitled moment";
		}
		String normalized = value.replaceAll("\\s+", " ").trim();
		return normalized.length() > 96 ? normalized.substring(0, 93) + "..." : normalized;
	}
}
