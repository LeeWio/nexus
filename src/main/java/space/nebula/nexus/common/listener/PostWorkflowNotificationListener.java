package space.nebula.nexus.common.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.enums.NotificationType;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.service.INotificationService;
import space.nebula.nexus.payload.response.NotificationContext;
import static space.nebula.nexus.payload.response.NotificationContext.ObjectType.POST;
import static space.nebula.nexus.payload.response.NotificationContext.Action.*;

@Component
@RequiredArgsConstructor
public class PostWorkflowNotificationListener {
	private final INotificationService notificationService;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onPostChanged(PostChangedEvent event) {
		var post = event.getPost();
		if (post.getAuthor() == null)
			return;
		String key = event.getNotificationEventId();
		String link = "/posts?id=" + post.getId();
		String title;
		String content;
		NotificationType type;
		switch (event.getChangeType()) {
			case SUBMITTED_FOR_REVIEW -> {
				notificationService.sendToPostReviewers("Post awaiting review",
						"\"" + post.getTitle() + "\" is ready for review.", link, key, post.getAuthor().getId(),
						new NotificationContext(POST, post.getId(), post.getAuthor().getId(), REVIEW));
				return;
			}
			case REJECTED -> {
				type = NotificationType.POST_REJECTED;
				title = "Post changes requested";
				content = "\"" + post.getTitle() + "\" was not approved. " + post.getReviewComment();
			}
			case PUBLISHED, CREATED -> {
				if (event.getChangeType() == PostChangeType.CREATED && post.getStatus() != PostStatus.PUBLISHED)
					return;
				type = NotificationType.POST_PUBLISHED;
				title = "Post published";
				content = "\"" + post.getTitle() + "\" is now published.";
				link = "/post/" + post.getSlug();
			}
			case SCHEDULED -> {
				type = NotificationType.POST_SCHEDULED;
				title = "Post publication scheduled";
				content = "\"" + post.getTitle() + "\" is scheduled for " + post.getScheduledAt() + ".";
			}
			case SCHEDULE_CANCELED -> {
				type = NotificationType.POST_SCHEDULE_CANCELED;
				title = "Publication schedule canceled";
				content = "The publication schedule for \"" + post.getTitle() + "\" was canceled.";
			}
			case ARCHIVED -> {
				type = NotificationType.POST_ARCHIVED;
				title = "Post archived";
				content = "\"" + post.getTitle() + "\" was archived. " + post.getArchiveReason();
			}
			default -> {
				return;
			}
		}
		notificationService.sendOnce(post.getAuthor(), title, content, type, link, key, new NotificationContext(POST,
				post.getId(), event.getNotificationActorId(), type == NotificationType.POST_REJECTED ? EDIT : VIEW));
	}
}
