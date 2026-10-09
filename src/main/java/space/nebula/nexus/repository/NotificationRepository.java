package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Notification;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
	Optional<Notification> findByDeduplicationKey(String deduplicationKey);

	default int insertOnce(Long userId, String title, String content, String type, String link, boolean visible,
			String eventKey) {
		return insertContextualOnce(userId, title, content, type, link, visible, eventKey, null, null, null, null);
	}

	@Modifying
	@Query(value = "INSERT INTO sys_notification "
			+ "(user_id, title, content, type, link, is_read, is_visible, is_saved, deduplication_key, created_at, updated_at, is_deleted, object_type, object_id, actor_id, action) "
			+ "VALUES (:userId, :title, :content, :type, :link, false, :visible, false, :eventKey, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), false, :objectType, :objectId, :actorId, :action) "
			+ "ON DUPLICATE KEY UPDATE id = id", nativeQuery = true)
	int insertContextualOnce(Long userId, String title, String content, String type, String link, boolean visible,
			String eventKey, String objectType, Long objectId, Long actorId, String action);
	Page<Notification> findByRecipientId(Long userId, Pageable pageable);
	Page<Notification> findByRecipientIdAndIsReadFalse(Long userId, Pageable pageable);

	String CATEGORY_MATCH = "(:category IS NULL OR CASE n.type "
			+ "WHEN 'COMMENT_APPROVED' THEN 'COMMENT' WHEN 'COMMENT_REJECTED' THEN 'COMMENT' "
			+ "WHEN 'COMMENT_REPLY' THEN 'COMMENT' WHEN 'POST_COMMENT' THEN 'COMMENT' "
			+ "WHEN 'MOMENT_COMMENT' THEN 'COMMENT' WHEN 'CATEGORY_POST' THEN 'CATEGORY_POST' "
			+ "WHEN 'POST_REJECTED' THEN 'CREATOR' WHEN 'POST_PUBLISHED' THEN 'CREATOR' "
			+ "WHEN 'POST_SCHEDULED' THEN 'CREATOR' WHEN 'POST_SCHEDULE_CANCELED' THEN 'CREATOR' "
			+ "WHEN 'POST_ARCHIVED' THEN 'CREATOR' WHEN 'COMMENT_PENDING_REVIEW' THEN 'MODERATION' "
			+ "WHEN 'COMMENT_FLAGGED' THEN 'MODERATION' WHEN 'POST_PENDING_REVIEW' THEN 'MODERATION' "
			+ "WHEN 'FRIEND_LINK_APPLICATION' THEN 'MODERATION' WHEN 'GUESTBOOK_COMMENT' THEN 'MODERATION' "
			+ "WHEN 'USER_PENDING_REVIEW' THEN 'MODERATION' WHEN 'COMMENT_REPORT_RECEIVED' THEN 'REPORT' "
			+ "WHEN 'COMMENT_REPORT_RESOLVED' THEN 'REPORT' WHEN 'POST_REPORT_RECEIVED' THEN 'REPORT' "
			+ "WHEN 'POST_REPORT_RESOLVED' THEN 'REPORT' ELSE 'OPERATIONS' END = :category)";

	@Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId "
			+ "AND n.isVisible = true AND n.completedAt IS NULL AND (:unreadOnly = false OR n.isRead = false) AND "
			+ CATEGORY_MATCH)
	Page<Notification> findInboxByRecipientId(Long userId, boolean unreadOnly, String category, Pageable pageable);

	@Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId "
			+ "AND n.isVisible = true AND n.completedAt IS NULL AND n.isSaved = true "
			+ "AND (:unreadOnly = false OR n.isRead = false) AND " + CATEGORY_MATCH)
	Page<Notification> findSavedByRecipientId(Long userId, boolean unreadOnly, String category, Pageable pageable);

	@Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId "
			+ "AND n.isVisible = true AND n.completedAt IS NOT NULL AND (:unreadOnly = false OR n.isRead = false) AND "
			+ CATEGORY_MATCH)
	Page<Notification> findDoneByRecipientId(Long userId, boolean unreadOnly, String category, Pageable pageable);
	@Query("SELECT n FROM Notification n WHERE n.id = :id AND n.recipient.id = :userId AND n.isVisible = true")
	Optional<Notification> findByIdAndRecipientId(Long id, Long userId);
	long countByRecipientIdAndIsVisibleTrueAndIsReadFalse(Long userId);

	@Query("SELECT notification FROM Notification notification JOIN FETCH notification.recipient recipient "
			+ "LEFT JOIN NotificationPreference preference ON preference.user = recipient AND preference.isDeleted = false "
			+ "LEFT JOIN NotificationCategoryPreference categoryPreference ON categoryPreference.userId = recipient.id AND categoryPreference.category = space.nebula.nexus.enums.NotificationCategory.CATEGORY_POST "
			+ "WHERE notification.deduplicationKey LIKE CONCAT(:deduplicationPrefix, '%') "
			+ "AND recipient.status = 'ACTIVE' AND COALESCE(categoryPreference.emailEnabled, preference.categoryPostEmailEnabled, false) = true")
	List<Notification> findCategoryPublicationEmailNotifications(String deduplicationPrefix);

	@Modifying
	@Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP "
			+ "WHERE n.recipient.id = :userId AND n.isVisible = true AND n.isRead = false")
	int markAllAsRead(Long userId);

	@Modifying
	@Query("UPDATE Notification n SET n.isVisible = false WHERE n.id = :id AND n.recipient.id = :userId AND n.isVisible = true")
	int deleteOwnedById(Long id, Long userId);

	@Modifying
	@Query("UPDATE Notification n SET n.isVisible = false WHERE n.recipient.id = :userId AND n.isVisible = true "
			+ "AND n.completedAt IS NULL AND n.isSaved = false AND n.isRead = true")
	int deleteReadByRecipientId(Long userId);

	/**
	 * Creates one idempotent notification for every active follower of a category.
	 *
	 * @param categoryId
	 *            category identifier
	 * @param authorId
	 *            post author identifier excluded from recipients
	 * @param postId
	 *            published post identifier used for deduplication
	 * @param title
	 *            notification title
	 * @param content
	 *            notification content
	 * @param link
	 *            application link to the post
	 * @return number of notifications inserted
	 */
	@Modifying
	@Query(value = "INSERT IGNORE INTO sys_notification "
			+ "(user_id, title, content, type, is_read, is_visible, link, deduplication_key, created_at, updated_at, is_deleted, object_type, object_id, actor_id, action) "
			+ "SELECT follow.user_id, :title, :content, 'CATEGORY_POST', false, "
			+ "COALESCE(category_preference.in_app_enabled, preference.category_post_enabled, true), :link, "
			+ "CONCAT('CATEGORY_POST:', :postId, ':', follow.user_id), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), false, 'POST', :postId, :authorId, 'VIEW' "
			+ "FROM blog_category_follow follow JOIN sys_user user_account ON user_account.id = follow.user_id "
			+ "LEFT JOIN sys_notification_preference preference ON preference.user_id = follow.user_id "
			+ "AND preference.is_deleted = false "
			+ "LEFT JOIN sys_notification_category_preference category_preference ON category_preference.user_id = follow.user_id AND category_preference.category = 'CATEGORY_POST' "
			+ "WHERE follow.category_id = :categoryId AND follow.is_deleted = false "
			+ "AND user_account.is_deleted = false AND user_account.status = 'ACTIVE' "
			+ "AND (COALESCE(category_preference.in_app_enabled, preference.category_post_enabled, true) = true "
			+ "OR COALESCE(category_preference.email_enabled, preference.category_post_email_enabled, false) = true) "
			+ "AND follow.user_id <> :authorId", nativeQuery = true)
	int insertCategoryPublicationNotifications(Long categoryId, Long authorId, Long postId, String title,
			String content, String link);
}
