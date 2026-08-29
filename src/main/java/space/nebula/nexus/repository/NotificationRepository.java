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
	Page<Notification> findByRecipientId(Long userId, Pageable pageable);
	Page<Notification> findByRecipientIdAndIsReadFalse(Long userId, Pageable pageable);

	@Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId "
			+ "AND n.isVisible = true AND n.completedAt IS NULL AND (:unreadOnly = false OR n.isRead = false)")
	Page<Notification> findInboxByRecipientId(Long userId, boolean unreadOnly, Pageable pageable);

	@Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId "
			+ "AND n.isVisible = true AND n.completedAt IS NULL AND n.isSaved = true AND (:unreadOnly = false OR n.isRead = false)")
	Page<Notification> findSavedByRecipientId(Long userId, boolean unreadOnly, Pageable pageable);

	@Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId "
			+ "AND n.isVisible = true AND n.completedAt IS NOT NULL AND (:unreadOnly = false OR n.isRead = false)")
	Page<Notification> findDoneByRecipientId(Long userId, boolean unreadOnly, Pageable pageable);
	Optional<Notification> findByIdAndRecipientId(Long id, Long userId);
	long countByRecipientIdAndIsVisibleTrueAndIsReadFalse(Long userId);

	@Query("SELECT notification FROM Notification notification JOIN FETCH notification.recipient recipient "
			+ "JOIN NotificationPreference preference ON preference.user = recipient AND preference.isDeleted = false "
			+ "WHERE notification.deduplicationKey LIKE CONCAT(:deduplicationPrefix, '%') "
			+ "AND preference.categoryPostEmailEnabled = true")
	List<Notification> findCategoryPublicationEmailNotifications(String deduplicationPrefix);

	@Modifying
	@Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP "
			+ "WHERE n.recipient.id = :userId AND n.isVisible = true AND n.isRead = false")
	int markAllAsRead(Long userId);

	@Modifying
	@Query("DELETE FROM Notification n WHERE n.id = :id AND n.recipient.id = :userId")
	int deleteOwnedById(Long id, Long userId);

	@Modifying
	@Query("DELETE FROM Notification n WHERE n.recipient.id = :userId AND n.isVisible = true "
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
			+ "(user_id, title, content, type, is_read, is_visible, link, deduplication_key, created_at, updated_at, is_deleted) "
			+ "SELECT follow.user_id, :title, :content, 'CATEGORY_POST', false, "
			+ "COALESCE(preference.category_post_enabled, true), :link, "
			+ "CONCAT('CATEGORY_POST:', :postId, ':', follow.user_id), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), false "
			+ "FROM blog_category_follow follow JOIN sys_user user_account ON user_account.id = follow.user_id "
			+ "LEFT JOIN sys_notification_preference preference ON preference.user_id = follow.user_id "
			+ "AND preference.is_deleted = false "
			+ "WHERE follow.category_id = :categoryId AND follow.is_deleted = false "
			+ "AND user_account.is_deleted = false AND user_account.status = 'ACTIVE' "
			+ "AND (COALESCE(preference.category_post_enabled, true) = true "
			+ "OR COALESCE(preference.category_post_email_enabled, false) = true) "
			+ "AND follow.user_id <> :authorId", nativeQuery = true)
	int insertCategoryPublicationNotifications(Long categoryId, Long authorId, Long postId, String title,
			String content, String link);
}
