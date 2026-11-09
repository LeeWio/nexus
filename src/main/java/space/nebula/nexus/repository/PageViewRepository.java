package space.nebula.nexus.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PageView;
import space.nebula.nexus.enums.TrafficClass;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface PageViewRepository extends JpaRepository<PageView, Long> {

	@Modifying
	@Query("DELETE FROM PageView v WHERE v.viewedAt < :cutoff")
	int deleteByViewedAtBefore(LocalDateTime cutoff);

	@Query("SELECT COUNT(v) FROM PageView v WHERE v.viewedAt >= :start AND v.viewedAt < :end "
			+ "AND v.trafficClass IN :classes")
	long countViews(LocalDateTime start, LocalDateTime end, Collection<TrafficClass> classes);

	@Query("SELECT COUNT(DISTINCT v.visitorHash) FROM PageView v WHERE v.viewedAt >= :start AND v.viewedAt < :end "
			+ "AND v.trafficClass IN :classes")
	long countVisitors(LocalDateTime start, LocalDateTime end, Collection<TrafficClass> classes);

	@Query("SELECT COUNT(DISTINCT v.sessionId) FROM PageView v WHERE v.viewedAt >= :start AND v.viewedAt < :end "
			+ "AND v.trafficClass IN :classes")
	long countSessions(LocalDateTime start, LocalDateTime end, Collection<TrafficClass> classes);

	@Query(value = "SELECT COUNT(*) FROM (SELECT session_id FROM blog_page_view WHERE viewed_at >= :start "
			+ "AND viewed_at < :end AND is_deleted = 0 AND traffic_class IN (:classes) GROUP BY session_id "
			+ "HAVING COUNT(DISTINCT path) = 1) bounced", nativeQuery = true)
	long countBouncedSessions(LocalDateTime start, LocalDateTime end, Collection<String> classes);

	@Query(value = "SELECT COALESCE(AVG(gap_seconds), 0) FROM (SELECT TIMESTAMPDIFF(SECOND, viewed_at, "
			+ "LEAD(viewed_at) OVER (PARTITION BY session_id ORDER BY viewed_at)) AS gap_seconds "
			+ "FROM blog_page_view WHERE viewed_at >= :start AND viewed_at < :end AND is_deleted = 0 "
			+ "AND traffic_class IN (:classes)) gaps WHERE gap_seconds IS NOT NULL AND gap_seconds BETWEEN 0 AND 1800", nativeQuery = true)
	Double averageEngagedSeconds(LocalDateTime start, LocalDateTime end, Collection<String> classes);

	@Query(value = "SELECT DATE(viewed_at) AS visitDate, COUNT(*) AS pv, COUNT(DISTINCT session_id) AS sessions, "
			+ "COUNT(DISTINCT visitor_hash) AS users FROM blog_page_view WHERE viewed_at >= :start AND viewed_at < :end "
			+ "AND is_deleted = 0 AND traffic_class IN (:classes) GROUP BY DATE(viewed_at) ORDER BY DATE(viewed_at)", nativeQuery = true)
	List<java.util.Map<String, Object>> findDailyTrend(LocalDateTime start, LocalDateTime end,
			Collection<String> classes);

	@Query(value = "SELECT client_name AS name, COUNT(DISTINCT session_id) AS count FROM blog_page_view "
			+ "WHERE viewed_at >= :start AND viewed_at < :end AND is_deleted = 0 AND traffic_class IN (:classes) "
			+ "GROUP BY client_name ORDER BY count DESC", nativeQuery = true)
	List<java.util.Map<String, Object>> countByClient(LocalDateTime start, LocalDateTime end,
			Collection<String> classes);

	@Query(value = "SELECT device_type AS name, COUNT(DISTINCT session_id) AS count FROM blog_page_view "
			+ "WHERE viewed_at >= :start AND viewed_at < :end AND is_deleted = 0 AND traffic_class IN (:classes) "
			+ "GROUP BY device_type ORDER BY count DESC", nativeQuery = true)
	List<java.util.Map<String, Object>> countByDevice(LocalDateTime start, LocalDateTime end,
			Collection<String> classes);

	@Query(value = "SELECT os_name AS name, COUNT(DISTINCT session_id) AS count FROM blog_page_view "
			+ "WHERE viewed_at >= :start AND viewed_at < :end AND is_deleted = 0 AND traffic_class IN (:classes) "
			+ "GROUP BY os_name ORDER BY count DESC", nativeQuery = true)
	List<java.util.Map<String, Object>> countByOs(LocalDateTime start, LocalDateTime end, Collection<String> classes);

	@Query(value = "SELECT source_channel AS name, COUNT(DISTINCT session_id) AS count FROM blog_page_view "
			+ "WHERE viewed_at >= :start AND viewed_at < :end AND is_deleted = 0 AND traffic_class IN (:classes) "
			+ "GROUP BY source_channel ORDER BY count DESC", nativeQuery = true)
	List<java.util.Map<String, Object>> countByChannel(LocalDateTime start, LocalDateTime end,
			Collection<String> classes);

	@Query(value = "SELECT path, COUNT(*) AS views, COUNT(DISTINCT session_id) AS sessions, "
			+ "COUNT(DISTINCT visitor_hash) AS visitors FROM blog_page_view WHERE viewed_at >= :start "
			+ "AND viewed_at < :end AND is_deleted = 0 AND traffic_class IN (:classes) GROUP BY path "
			+ "ORDER BY views DESC LIMIT 20", nativeQuery = true)
	List<java.util.Map<String, Object>> findTopPaths(LocalDateTime start, LocalDateTime end,
			Collection<String> classes);

	@Query(value = "SELECT visitor_hash AS visitorHash, MAX(ip_address) AS ipAddress, MAX(country) AS country, "
			+ "MAX(region) AS region, MAX(city) AS city, MAX(client_name) AS clientName, MAX(os_name) AS osName, "
			+ "MAX(device_type) AS deviceType, MIN(viewed_at) AS firstSeen, MAX(viewed_at) AS lastSeen, "
			+ "COUNT(*) AS views, COUNT(DISTINCT session_id) AS sessions, COUNT(DISTINCT path) AS pages "
			+ "FROM blog_page_view WHERE viewed_at >= :start AND viewed_at < :end AND is_deleted = 0 "
			+ "AND traffic_class IN (:classes) GROUP BY visitor_hash ORDER BY lastSeen DESC", nativeQuery = true)
	List<java.util.Map<String, Object>> findVisitors(LocalDateTime start, LocalDateTime end, Collection<String> classes,
			Pageable pageable);

	@Query(value = "SELECT COUNT(DISTINCT visitor_hash) FROM blog_page_view WHERE viewed_at >= :start "
			+ "AND viewed_at < :end AND is_deleted = 0 AND traffic_class IN (:classes)", nativeQuery = true)
	long countVisitorRows(LocalDateTime start, LocalDateTime end, Collection<String> classes);

	List<PageView> findByVisitorHashAndViewedAtGreaterThanEqualAndViewedAtLessThanAndTrafficClassInOrderByViewedAtDesc(
			String visitorHash, LocalDateTime start, LocalDateTime end, Collection<TrafficClass> classes,
			Pageable pageable);

	@Query(value = "SELECT COUNT(DISTINCT current_view.visitor_hash) FROM blog_page_view current_view "
			+ "WHERE current_view.viewed_at >= :start AND current_view.viewed_at < :end AND current_view.is_deleted = 0 "
			+ "AND current_view.traffic_class IN (:classes) AND EXISTS (SELECT 1 FROM blog_page_view prior_view "
			+ "WHERE prior_view.visitor_hash = current_view.visitor_hash AND prior_view.viewed_at < :start "
			+ "AND prior_view.is_deleted = 0)", nativeQuery = true)
	long countReturningVisitors(LocalDateTime start, LocalDateTime end, Collection<String> classes);
}
