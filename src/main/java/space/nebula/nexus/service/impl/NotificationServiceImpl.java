package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Notification;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.NotificationResponse;
import space.nebula.nexus.repository.NotificationRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.INotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

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
    @Transactional(readOnly = true)
    public ApiResponse<PageResult<NotificationResponse>> getMyNotifications(Pageable pageable) {
        User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
        var notifications = notificationRepository.findByRecipientId(currentUser.getId(), pageable);
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
			notificationRepository.save(notification);
		}
        return ApiResponse.success("Notification marked as read", null);
    }

    @Override
    @Transactional
    public ApiResponse<Void> markAllAsRead() {
        User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
        notificationRepository.markAllAsRead(currentUser.getId());
        return ApiResponse.success("All notifications marked as read", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Long> getUnreadCount() {
        User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
        long count = notificationRepository.countByRecipientIdAndIsReadFalse(currentUser.getId());
        return ApiResponse.success(count);
    }

	private NotificationResponse toResponse(Notification notification) {
		return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getContent(),
				notification.getType(), notification.getIsRead(), notification.getLink(), notification.getCreatedAt());
	}
}
