package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.response.NotificationResponse;
import space.nebula.nexus.payload.response.PageResult;

public interface INotificationService {

    /**
     * Sends a notification to a specific user.
     */
    void send(User recipient, String title, String content, String type, String link);

    /**
     * Retrieves notifications for the current user.
     */
    ApiResponse<PageResult<NotificationResponse>> getMyNotifications(Pageable pageable);

    /**
     * Marks a specific notification as read.
     */
    ApiResponse<Void> markAsRead(Long id);

    /**
     * Marks all notifications as read for the current user.
     */
    ApiResponse<Void> markAllAsRead();

    /**
     * Gets the count of unread notifications for the current user.
     */
    ApiResponse<Long> getUnreadCount();
}
