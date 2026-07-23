package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Periodically reconciles denormalized post interaction counters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionSyncTask {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * Reconciles denormalized counters from the durable interaction relation
	 * tables.
	 */
	@Scheduled(fixedRate = 120000) // Runs every 2 minutes
	@SchedulerLock(name = "interactionSync", lockAtMostFor = "PT110S")
	@Transactional
	public void synchronizeSocialInteractions() {
		int updated = jdbcTemplate.update("UPDATE blog_post p "
				+ "LEFT JOIN (SELECT post_id, COUNT(*) AS total FROM blog_post_like GROUP BY post_id) l ON l.post_id = p.id "
				+ "LEFT JOIN (SELECT post_id, COUNT(*) AS total FROM blog_post_favorite GROUP BY post_id) f ON f.post_id = p.id "
				+ "SET p.likes_count = COALESCE(l.total, 0), p.favorites_count = COALESCE(f.total, 0) "
				+ "WHERE p.is_deleted = false AND ("
				+ "p.likes_count <> COALESCE(l.total, 0) OR p.favorites_count <> COALESCE(f.total, 0))");
		log.info("Reconciled interaction counters for {} posts", updated);
	}
}
