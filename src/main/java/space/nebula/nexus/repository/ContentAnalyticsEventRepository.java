package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.ContentAnalyticsEvent;
import space.nebula.nexus.enums.ContentAnalyticsEventType;

import java.time.LocalDateTime;

@Repository
public interface ContentAnalyticsEventRepository extends JpaRepository<ContentAnalyticsEvent, Long> {

	boolean existsBySessionIdAndPostIdAndEventTypeAndIsDeletedFalse(String sessionId, Long postId,
			ContentAnalyticsEventType eventType);

	@Query("SELECT COUNT(DISTINCT event.sessionId) FROM ContentAnalyticsEvent event "
			+ "WHERE event.eventType = :eventType AND event.createdAt >= :start AND event.createdAt <= :end "
			+ "AND (:postId IS NULL OR event.postId = :postId)")
	long countDistinctSessionsByEventTypeAndPeriod(ContentAnalyticsEventType eventType, LocalDateTime start,
			LocalDateTime end, Long postId);

	@Query("SELECT COALESCE(AVG(event.activeSeconds), 0) FROM ContentAnalyticsEvent event "
			+ "WHERE event.eventType = :eventType " + "AND event.createdAt >= :start AND event.createdAt <= :end "
			+ "AND (:postId IS NULL OR event.postId = :postId)")
	Double findAverageActiveSecondsByEventTypeAndPeriod(ContentAnalyticsEventType eventType, LocalDateTime start,
			LocalDateTime end, Long postId);

	long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
