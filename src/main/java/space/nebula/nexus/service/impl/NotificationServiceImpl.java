package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Notification;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.NotificationResponse;
import space.nebula.nexus.repository.NotificationRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.INotificationService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final PostRepository postRepository;

	@Override
	@Transactional
	public void send(User recipient, String title, String content, String type, String link) {
		Notification notification = new Notification();
		notification.setRecipient(recipient);
		notification.setTitle(title);
		notification.setContent(content);
		notification.setType(type);
		notification.setLink(link);
		notificationRepository.save(notification);
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
		log.info("Created {} category publication notifications for post {}", inserted, postId);
		return inserted;
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<NotificationResponse>> getMyNotifications(boolean unreadOnly, Pageable pageable) {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		var notifications = unreadOnly
				? notificationRepository.findByRecipientIdAndIsReadFalse(currentUser.getId(), pageable)
				: notificationRepository.findByRecipientId(currentUser.getId(), pageable);
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
	@Transactional(readOnly = true)
	public ApiResponse<Long> getUnreadCount() {
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		long count = notificationRepository.countByRecipientIdAndIsReadFalse(currentUser.getId());
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
				notification.getType(), notification.getIsRead(), notification.getReadAt(), notification.getLink(),
				notification.getCreatedAt());
	}
}
