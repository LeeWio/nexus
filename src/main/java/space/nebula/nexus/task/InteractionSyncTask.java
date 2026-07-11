package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionSyncTask
{

	private final JdbcTemplate jdbcTemplate;

	/**
	 * Reconciles denormalized counters from the durable interaction relation tables.
	 */
	@Scheduled(fixedRate = 120000) // Runs every 2 minutes
	@SchedulerLock(name = "interactionSync", lockAtMostFor = "PT110S")
	@Transactional
	public void synchronizeSocialInteractions()
	{
		int updated = jdbcTemplate.update("UPDATE blog_post p SET "
				+ "likes_count = (SELECT COUNT(*) FROM blog_post_like l WHERE l.post_id = p.id), "
				+ "favorites_count = (SELECT COUNT(*) FROM blog_post_favorite f WHERE f.post_id = p.id) "
				+ "WHERE p.is_deleted = false AND ("
				+ "EXISTS (SELECT 1 FROM blog_post_like l2 WHERE l2.post_id = p.id) OR "
				+ "EXISTS (SELECT 1 FROM blog_post_favorite f2 WHERE f2.post_id = p.id))");
		log.info("Reconciled interaction counters for {} posts", updated);
	}
}
