package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.VisitLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

	@Query("SELECT COUNT(v) FROM VisitLog v WHERE v.visitTime >= :start AND v.visitTime <= :end")
	long countPv(LocalDateTime start, LocalDateTime end);

	@Query("SELECT COUNT(DISTINCT v.visitorHash) FROM VisitLog v WHERE v.visitTime >= :start AND v.visitTime <= :end")
	long countUv(LocalDateTime start, LocalDateTime end);

	@Query("SELECT COUNT(DISTINCT v.sessionId) FROM VisitLog v WHERE v.visitTime >= :start AND v.visitTime <= :end")
	long countSessions(LocalDateTime start, LocalDateTime end);

	@Query(value = "SELECT COUNT(*) FROM (SELECT session_id FROM sys_visit_log "
			+ "WHERE visit_time >= :start AND visit_time <= :end AND is_deleted = false "
			+ "GROUP BY session_id HAVING COUNT(DISTINCT request_url) = 1) bounced_sessions", nativeQuery = true)
	long countBouncedSessions(LocalDateTime start, LocalDateTime end);

	@Query(value = "SELECT COALESCE(AVG(session_duration), 0) FROM (SELECT TIMESTAMPDIFF(SECOND, MIN(visit_time), "
			+ "MAX(visit_time)) AS session_duration FROM sys_visit_log WHERE visit_time >= :start AND visit_time <= :end "
			+ "AND is_deleted = false GROUP BY session_id) session_durations", nativeQuery = true)
	Double findAverageSessionDurationSeconds(LocalDateTime start, LocalDateTime end);

	@Query(value = "SELECT COUNT(DISTINCT current_log.visitor_hash) FROM sys_visit_log current_log "
			+ "WHERE current_log.visit_time >= :start AND current_log.visit_time <= :end AND current_log.is_deleted = false "
			+ "AND EXISTS (SELECT 1 FROM sys_visit_log prior_log WHERE prior_log.visitor_hash = current_log.visitor_hash "
			+ "AND prior_log.visit_time < :start AND prior_log.is_deleted = false)", nativeQuery = true)
	long countReturningVisitors(LocalDateTime start, LocalDateTime end);

	@Query(value = "SELECT COALESCE(AVG(session_duration), 0) FROM (SELECT TIMESTAMPDIFF(SECOND, MIN(visit_time), "
			+ "MAX(visit_time)) AS session_duration FROM sys_visit_log WHERE request_url = :url AND visit_time >= :start "
			+ "AND visit_time <= :end AND is_deleted = false GROUP BY session_id) session_durations", nativeQuery = true)
	Double findAverageSessionDurationSecondsForPath(String url, LocalDateTime start, LocalDateTime end);

	@Query(value = "SELECT COUNT(*) FROM (SELECT session_id FROM sys_visit_log WHERE request_url = :url "
			+ "AND visit_time >= :start AND visit_time <= :end AND is_deleted = false GROUP BY session_id "
			+ "HAVING COUNT(DISTINCT request_url) = 1) bounced_sessions", nativeQuery = true)
	long countBouncedSessionsForPath(String url, LocalDateTime start, LocalDateTime end);

	@Query("SELECT COUNT(DISTINCT v.sessionId) FROM VisitLog v WHERE v.requestUrl = :url "
			+ "AND v.visitTime >= :start AND v.visitTime <= :end")
	long countSessionsForPath(String url, LocalDateTime start, LocalDateTime end);

	@Query("SELECT v.requestUrl as url, COUNT(v) as count FROM VisitLog v WHERE v.visitTime >= :start GROUP BY v.requestUrl ORDER BY count DESC")
	List<Map<String, Object>> findTopContentRaw(LocalDateTime start);

	@Query("SELECT COUNT(v) FROM VisitLog v WHERE v.requestUrl LIKE '/api/v1/public/blog/posts/%' "
			+ "AND v.visitTime >= :start AND v.visitTime <= :end")
	long countPostDetailViews(LocalDateTime start, LocalDateTime end);

	@Query("SELECT v.requestUrl as url, COUNT(v) as count FROM VisitLog v WHERE v.visitTime >= :start AND v.visitTime <= :end GROUP BY v.requestUrl ORDER BY count DESC")
	List<Map<String, Object>> findTopContentRawByRange(LocalDateTime start, LocalDateTime end);

	@Query("SELECT v.os as os, COUNT(v) as count FROM VisitLog v WHERE v.visitTime >= :start AND v.visitTime <= :end GROUP BY v.os ORDER BY count DESC")
	List<Map<String, Object>> findDeviceStatsRawByRange(LocalDateTime start, LocalDateTime end);

	@Query("SELECT v.referer as referer, v.requestUrl as url, COUNT(v) as count FROM VisitLog v WHERE v.visitTime >= :start AND v.visitTime <= :end GROUP BY v.referer, v.requestUrl ORDER BY count DESC")
	List<Map<String, Object>> findSourceStatsRawByRange(LocalDateTime start, LocalDateTime end);

	@Query("SELECT DATE(v.visitTime) as visitDate, COUNT(v) as pv, COUNT(DISTINCT v.visitorHash) as uv FROM VisitLog v WHERE v.visitTime >= :start GROUP BY visitDate ORDER BY visitDate ASC")
	List<Map<String, Object>> findDailyTrendRaw(LocalDateTime start);

	@Query(value = "SELECT DATE(visit_time) AS visitDate, COUNT(DISTINCT session_id) AS sessions, "
			+ "COUNT(DISTINCT visitor_hash) AS users FROM sys_visit_log WHERE visit_time >= :start "
			+ "AND is_deleted = false GROUP BY DATE(visit_time) ORDER BY DATE(visit_time) ASC", nativeQuery = true)
	List<Map<String, Object>> findDailySessionTrendRaw(LocalDateTime start);

	@Modifying
	@Query("DELETE FROM VisitLog v WHERE v.visitTime < :time")
	int deleteByVisitTimeBefore(LocalDateTime time);
}
