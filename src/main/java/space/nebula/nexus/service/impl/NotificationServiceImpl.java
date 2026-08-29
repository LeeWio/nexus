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

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

	private final NotificationRepository notificationRepository;
	private final NotificationPreferenceRepository notificationPreferenceRepository;
	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final NotificationDeliveryService notificationDeliveryService;

	private static final Set<String> COMMENT_NOTIFICATION_TYPES = Set.of("COMMENT_APPROVED", "COMMENT_REJECTED",
			"COMMENT_REPLY", "POST_COMMENT");

	@Override
	@Transactional
	public void send(User recipient, String title, String content, String type, String link) {
		boolean inAppEnabled = isInAppNotificationEnabled(recipient.getId(), type);
		boolean emailEnabled = isEmailNotificationEnabled(recipient.getId(), type);
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
		if (emailEnabled) dispatchEmail(notification, recipient, title, content, link);
		log.debug("Notification sent to user {}: {}", recipient.getUsername(), title);
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
				.forEach(notification -> dispatchEmail(notification, notification.getRecipient(), notification.getTitle(),
						notification.getContent(), notification.getLink()));
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
	public ApiResponse<PageResult<NotificationResponse>> getMyNotifications(boolean unreadOnly, String view, Pageable pageable) {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		var notifications = switch (view.toLowerCase(java.util.Locale.ROOT)) {
			case "saved" -> notificationRepository.findSavedByRecipientId(currentUser.getId(), unreadOnly, pageable);
			case "done" -> notificationRepository.findDoneByRecipientId(currentUser.getId(), unreadOnly, pageable);
			default -> notificationRepository.findInboxByRecipientId(currentUser.getId(), unreadOnly, pageable);
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
			if (notification.getReadAt() == null) notification.setReadAt(LocalDateTime.now());
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
				notification.getType(), notification.getIsRead(), notification.getIsSaved(), notification.getReadAt(), notification.getCompletedAt(), notification.getLink(),
				notification.getCreatedAt());
	}

	private Notification findOwnedNotification(Long id) {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		return notificationRepository.findByIdAndRecipientId(id, currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
	}

	private boolean isInAppNotificationEnabled(Long recipientId, String type) {
		return notificationPreferenceRepository.findByUserIdAndIsDeletedFalse(recipientId)
				.map(preference -> switch (notificationCategory(type)) {
					case COMMENT -> Boolean.TRUE.equals(preference.getCommentEnabled());
					case CATEGORY_POST -> Boolean.TRUE.equals(preference.getCategoryPostEnabled());
					case SYSTEM -> Boolean.TRUE.equals(preference.getSystemEnabled());
		}).orElse(true);
	}

	private boolean isEmailNotificationEnabled(Long recipientId, String type) {
		return notificationPreferenceRepository.findByUserIdAndIsDeletedFalse(recipientId)
				.map(preference -> switch (notificationCategory(type)) {
					case COMMENT -> Boolean.TRUE.equals(preference.getCommentEmailEnabled());
					case CATEGORY_POST -> Boolean.TRUE.equals(preference.getCategoryPostEmailEnabled());
					case SYSTEM -> Boolean.TRUE.equals(preference.getSystemEmailEnabled());
				}).orElse(false);
	}

	private void dispatchEmail(Notification notification, User recipient, String title, String content, String link) {
		notificationDeliveryService.queueEmail(notification, recipient.getEmail(), title, content, link);
	}

	private NotificationCategory notificationCategory(String type) {
		if (COMMENT_NOTIFICATION_TYPES.contains(type)) {
			return NotificationCategory.COMMENT;
		}
		return "CATEGORY_POST".equals(type) ? NotificationCategory.CATEGORY_POST : NotificationCategory.SYSTEM;
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

	private enum NotificationCategory {
		COMMENT, CATEGORY_POST, SYSTEM
	}
}
