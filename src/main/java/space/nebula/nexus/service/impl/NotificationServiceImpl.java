package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Notification;
import space.nebula.nexus.entity.NotificationPreference;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.enums.NotificationType;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.request.NotificationPreferenceRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.NotificationPreferenceResponse;
import space.nebula.nexus.payload.response.NotificationResponse;
import space.nebula.nexus.repository.NotificationPreferenceRepository;
import space.nebula.nexus.repository.NotificationRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.INotificationService;
import space.nebula.nexus.service.NotificationDeliveryService;
import space.nebula.nexus.service.NotificationPreferenceService;
import space.nebula.nexus.service.NotificationContexts;
import space.nebula.nexus.repository.NotificationCategoryPreferenceRepository;
import space.nebula.nexus.enums.NotificationCategory;
import space.nebula.nexus.payload.response.NotificationContext;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

	private final NotificationRepository notificationRepository;
	private final NotificationPreferenceRepository notificationPreferenceRepository;
	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final NotificationDeliveryService notificationDeliveryService;
	private final NotificationCategoryPreferenceRepository categoryPreferenceRepository;

	@Override
	@Transactional
	public void sendOnce(User recipient, String title, String content, NotificationType type, String link,
			String eventKey) {
		sendOnce(recipient, title, content, type, link, eventKey, null);
	}

	@Override
	@Transactional
	public void sendOnce(User recipient, String title, String content, NotificationType type, String link,
			String eventKey, NotificationContext context) {
		if (recipient == null || recipient.getStatus() != UserStatus.ACTIVE
				|| Boolean.TRUE.equals(recipient.getIsDeleted()))
			return;
		String key = type.name() + ":" + eventKey + ":" + recipient.getId();
		if (key.length() > 150)
			throw new IllegalArgumentException("Notification event key is too long");
		// Dismissed notifications retain the key so replay cannot recreate them.
		if (notificationRepository.findByDeduplicationKey(key).isPresent())
			return;
		var channels = channels(recipient.getId(), type.name());
		boolean inAppEnabled = channels.inApp();
		boolean emailEnabled = channels.email();
		if (!inAppEnabled && !emailEnabled)
			return;
		if (context == null)
			notificationRepository.insertOnce(recipient.getId(), title, content, type.name(), link, inAppEnabled, key);
		else
			notificationRepository.insertContextualOnce(recipient.getId(), title, content, type.name(), link,
					inAppEnabled, key, context.objectType().name(), context.objectId(), context.actorId(),
					context.action().name());
		if (emailEnabled)
			notificationRepository.findByDeduplicationKey(key)
					.ifPresent(notification -> dispatchEmail(notification, recipient, title, content, link));
	}

	@Override
	@Transactional
	public void sendToAdministrators(String title, String content, NotificationType type, String link, String eventKey,
			Long actorId) {
		sendToAdministrators(title, content, type, link, eventKey, actorId, null);
	}

	@Override
	@Transactional
	public void sendToAdministrators(String title, String content, NotificationType type, String link, String eventKey,
			Long actorId, NotificationContext context) {
		userRepository.findActiveAdministrators().stream().filter(user -> !user.getId().equals(actorId))
				.forEach(user -> sendOnce(user, title, content, type, link, eventKey, context));
	}

	@Override
	@Transactional
	public void send(User recipient, String title, String content, String type, String link) {
		var channels = channels(recipient.getId(), type);
		boolean inAppEnabled = channels.inApp();
		boolean emailEnabled = channels.email();
		if (!inAppEnabled && !emailEnabled) {
			log.debug("Notification suppressed by delivery preference for user {}: {}", recipient.getUsername(), type);
			return;
		}
		Notification notification = new Notification();
		notification.setRecipient(recipient);
		notification.setTitle(title);
		notification.setContent(content);
		notification.setType(type);
		notification.setLink(link);
		notification.setIsVisible(inAppEnabled);
		notificationRepository.save(notification);
		if (emailEnabled)
			dispatchEmail(notification, recipient, title, content, link);
		log.debug("Notification sent to user {}: {}", recipient.getUsername(), title);
	}

	@Override
	@Transactional
	public void sendToPostReviewers(String title, String content, String link, String eventKey, Long authorId) {
		sendToPostReviewers(title, content, link, eventKey, authorId, null);
	}

	@Override
	@Transactional
	public void sendToPostReviewers(String title, String content, String link, String eventKey, Long authorId,
			NotificationContext context) {
		userRepository.findActivePostReviewers().stream().filter(user -> !user.getId().equals(authorId)).forEach(
				user -> sendOnce(user, title, content, NotificationType.POST_PENDING_REVIEW, link, eventKey, context));
	}

	@Override
	@Transactional
	public int sendCategoryPublication(Long postId) {
		var post = postRepository.findPublicationNotificationPost(postId).orElse(null);
		if (post == null || post.getStatus() != PostStatus.PUBLISHED || post.getCategory() == null) {
			return 0;
		}
		String categoryName = post.getCategory().getName();
		String title = "New post in " + categoryName;
		String content = "\"" + post.getTitle() + "\" is now available in a category you follow.";
		int inserted = notificationRepository.insertCategoryPublicationNotifications(post.getCategory().getId(),
				post.getAuthor().getId(), post.getId(), title, content, "/post/" + post.getSlug());
		notificationRepository.findCategoryPublicationEmailNotifications("CATEGORY_POST:" + post.getId() + ":")
				.forEach(notification -> dispatchEmail(notification, notification.getRecipient(),
						notification.getTitle(), notification.getContent(), notification.getLink()));
		log.info("Created {} category publication notifications for post {}", inserted, postId);
		return inserted;
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<NotificationPreferenceResponse> getMyPreferences() {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		return ApiResponse.success(notificationPreferenceRepository.findByUserIdAndIsDeletedFalse(currentUser.getId())
				.map(this::toPreferenceResponse).orElseGet(NotificationServiceImpl::defaultPreferenceResponse));
	}

	@Override
	@Transactional
	public ApiResponse<NotificationPreferenceResponse> updateMyPreferences(NotificationPreferenceRequest request) {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		userRepository.findNotificationPreferenceOwnerForUpdate(currentUser.getId()).orElseThrow();
		NotificationPreference preference = notificationPreferenceRepository.findByUserId(currentUser.getId())
				.orElseGet(() -> {
					NotificationPreference created = new NotificationPreference();
					created.setUser(currentUser);
					return created;
				});
		preference.setIsDeleted(false);
		preference.setCommentEnabled(request.commentNotificationsEnabled());
		preference.setCategoryPostEnabled(request.categoryPostNotificationsEnabled());
		preference.setSystemEnabled(request.systemNotificationsEnabled());
		if (request.commentEmailNotificationsEnabled() != null) {
			preference.setCommentEmailEnabled(request.commentEmailNotificationsEnabled());
		}
		if (request.categoryPostEmailNotificationsEnabled() != null) {
			preference.setCategoryPostEmailEnabled(request.categoryPostEmailNotificationsEnabled());
		}
		if (request.systemEmailNotificationsEnabled() != null) {
			preference.setSystemEmailEnabled(request.systemEmailNotificationsEnabled());
		}
		return ApiResponse.success(toPreferenceResponse(notificationPreferenceRepository.save(preference)));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<NotificationResponse>> getMyNotifications(boolean unreadOnly, String view,
			NotificationCategory category, Pageable pageable) {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		String categoryName = category == null ? null : category.name();
		var notifications = switch (view.toLowerCase(java.util.Locale.ROOT)) {
			case "saved" -> notificationRepository.findSavedByRecipientId(currentUser.getId(), unreadOnly,
					categoryName, pageable);
			case "done" -> notificationRepository.findDoneByRecipientId(currentUser.getId(), unreadOnly, categoryName,
					pageable);
			default -> notificationRepository.findInboxByRecipientId(currentUser.getId(), unreadOnly, categoryName,
					pageable);
		};
		return ApiResponse.success(PageResult.of(notifications.map(this::toResponse)));
	}

	@Override
	@Transactional
	public ApiResponse<Void> markAsRead(Long id) {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		Notification notification = notificationRepository.findByIdAndRecipientId(id, currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

		if (!Boolean.TRUE.equals(notification.getIsRead())) {
			notification.setIsRead(true);
			notification.setReadAt(LocalDateTime.now());
			notificationRepository.save(notification);
		}
		return ApiResponse.success("Notification marked as read", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> markAllAsRead() {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		int updatedCount = notificationRepository.markAllAsRead(currentUser.getId());
		log.debug("Marked {} notifications as read for user {}", updatedCount, currentUser.getUsername());
		return ApiResponse.success("All notifications marked as read", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> markAsDone(Long id) {
		Notification notification = findOwnedNotification(id);
		if (notification.getCompletedAt() == null) {
			notification.setCompletedAt(LocalDateTime.now());
			notification.setIsRead(true);
			if (notification.getReadAt() == null)
				notification.setReadAt(LocalDateTime.now());
			notificationRepository.save(notification);
		}
		return ApiResponse.success("Notification completed", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> reopen(Long id) {
		Notification notification = findOwnedNotification(id);
		if (notification.getCompletedAt() != null) {
			notification.setCompletedAt(null);
			notificationRepository.save(notification);
		}
		return ApiResponse.success("Notification reopened", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> setSaved(Long id, boolean saved) {
		Notification notification = findOwnedNotification(id);
		notification.setIsSaved(saved);
		notificationRepository.save(notification);
		return ApiResponse.success(saved ? "Notification saved" : "Notification unsaved", null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<Long> getUnreadCount() {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		long count = notificationRepository.countByRecipientIdAndIsVisibleTrueAndIsReadFalse(currentUser.getId());
		return ApiResponse.success(count);
	}

	@Override
	@Transactional
	public ApiResponse<Void> deleteNotification(Long id) {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		if (notificationRepository.deleteOwnedById(id, currentUser.getId()) == 0) {
			throw new ResourceNotFoundException("Notification", "id", id);
		}
		return ApiResponse.success("Notification deleted successfully", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> clearReadNotifications() {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		int deletedCount = notificationRepository.deleteReadByRecipientId(currentUser.getId());
		log.debug("Deleted {} read notifications for user {}", deletedCount, currentUser.getUsername());
		return ApiResponse.success("Read notifications cleared successfully", null);
	}

	private NotificationResponse toResponse(Notification notification) {
		return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getContent(),
				notification.getType(), notification.getIsRead(), notification.getIsSaved(), notification.getReadAt(),
				notification.getCompletedAt(), notification.getLink(), notification.getCreatedAt(),
				NotificationContexts.from(notification));
	}

	private Notification findOwnedNotification(Long id) {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		return notificationRepository.findByIdAndRecipientId(id, currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
	}

	private NotificationPreferenceService.Channels channels(Long recipientId, String type) {
		NotificationCategory category = NotificationCategory.forType(type);
		return categoryPreferenceRepository.findByUserIdAndCategory(recipientId, category)
				.map(row -> new NotificationPreferenceService.Channels(row.getInAppEnabled(), row.getEmailEnabled()))
				.orElseGet(() -> NotificationPreferenceService.inherited(
						notificationPreferenceRepository.findByUserIdAndIsDeletedFalse(recipientId).orElse(null),
						category));
	}

	private void dispatchEmail(Notification notification, User recipient, String title, String content, String link) {
		notificationDeliveryService.queueEmail(notification, recipient.getEmail(), title, content, link);
	}

	private NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference) {
		return new NotificationPreferenceResponse(Boolean.TRUE.equals(preference.getCommentEnabled()),
				Boolean.TRUE.equals(preference.getCategoryPostEnabled()),
				Boolean.TRUE.equals(preference.getSystemEnabled()),
				Boolean.TRUE.equals(preference.getCommentEmailEnabled()),
				Boolean.TRUE.equals(preference.getCategoryPostEmailEnabled()),
				Boolean.TRUE.equals(preference.getSystemEmailEnabled()));
	}

	private static NotificationPreferenceResponse defaultPreferenceResponse() {
		return new NotificationPreferenceResponse(true, true, true, false, false, false);
	}

}
