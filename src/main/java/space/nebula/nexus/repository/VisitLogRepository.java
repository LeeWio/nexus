package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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

	@Query("SELECT COUNT(DISTINCT v.ipAddress) FROM VisitLog v WHERE v.visitTime >= :start AND v.visitTime <= :end")
	long countUv(LocalDateTime start, LocalDateTime end);

	@Query("SELECT v.requestUrl as url, COUNT(v) as count FROM VisitLog v WHERE v.visitTime >= :start GROUP BY v.requestUrl ORDER BY count DESC")
	List<Map<String, Object>> findTopContentRaw(LocalDateTime start);

	@Query("SELECT DATE(v.visitTime) as visitDate, COUNT(v) as pv, COUNT(DISTINCT v.ipAddress) as uv FROM VisitLog v WHERE v.visitTime >= :start GROUP BY visitDate ORDER BY visitDate ASC")
	List<Map<String, Object>> findDailyTrendRaw(LocalDateTime start);
}
