package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.response.NotificationResponse;
import space.nebula.nexus.payload.response.PageResult;

public interface INotificationService {

	/**
	 * Sends a notification to a specific user.
	 *
	 * @param recipient
	 *            user who owns the notification
	 * @param title
	 *            concise notification title
	 * @param content
	 *            notification details
	 * @param type
	 *            notification category
	 * @param link
	 *            optional application link related to the notification
	 */
	void send(User recipient, String title, String content, String type, String link);

	/**
	 * Notifies active category followers about a newly published post.
	 *
	 * @param postId
	 *            published post identifier
	 * @return number of notifications created
	 */
	int sendCategoryPublication(Long postId);

	/**
	 * Retrieves notifications for the current user.
	 *
	 * @param unreadOnly
	 *            whether to return only unread notifications
	 * @param pageable
	 *            pagination and sorting parameters
	 * @return paginated notifications owned by the current user
	 */
	ApiResponse<PageResult<NotificationResponse>> getMyNotifications(boolean unreadOnly, Pageable pageable);

	/**
	 * Marks a specific notification as read.
	 *
	 * @param id
	 *            notification identifier
	 * @return successful response after the notification is marked as read
	 */
	ApiResponse<Void> markAsRead(Long id);

	/**
	 * Marks all notifications as read for the current user.
	 *
	 * @return successful response after unread notifications are updated
	 */
	ApiResponse<Void> markAllAsRead();

	/**
	 * Gets the count of unread notifications for the current user.
	 *
	 * @return unread notification count
	 */
	ApiResponse<Long> getUnreadCount();

	/**
	 * Deletes a notification owned by the current user.
	 *
	 * @param id
	 *            notification identifier
	 * @return successful response when the notification was deleted
	 */
	ApiResponse<Void> deleteNotification(Long id);

	/**
	 * Deletes all read notifications owned by the current user.
	 *
	 * @return successful response after read notifications are cleared
	 */
	ApiResponse<Void> clearReadNotifications();
}
