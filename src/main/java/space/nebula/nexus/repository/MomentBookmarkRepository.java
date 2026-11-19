package space.nebula.nexus.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MomentBookmarkRepository {

	static final String VISIBLE_MOMENT = """
			m.is_deleted = false AND (m.visibility = 'PUBLIC' OR m.created_by = :username)
			""";
	static final String VISIBLE_BOOKMARKS = """
			FROM blog_moment_bookmark b
			JOIN blog_moment m ON m.id = b.moment_id
			WHERE b.user_id = :userId AND b.saved_at IS NOT NULL AND
			""" + VISIBLE_MOMENT;

	private final JdbcClient jdbcClient;

	public List<Long> findVisibleIds(Long userId, String username) {
		return jdbcClient.sql("SELECT m.id " + VISIBLE_BOOKMARKS).param("userId", userId).param("username", username)
				.query(Long.class).list();
	}

	public void save(Long userId, Long momentId) {
		jdbcClient.sql("""
				INSERT INTO blog_moment_bookmark (user_id, moment_id, saved_at)
				VALUES (:userId, :momentId, CURRENT_TIMESTAMP(6))
				ON DUPLICATE KEY UPDATE saved_at = COALESCE(saved_at, CURRENT_TIMESTAMP(6))
				""").param("userId", userId).param("momentId", momentId).update();
	}

	public void remove(Long userId, String username, Long momentId) {
		// A now-hidden bookmark can still be removed by its owner.
		jdbcClient.sql("""
				INSERT INTO blog_moment_bookmark (user_id, moment_id, saved_at)
				SELECT :userId, m.id, NULL FROM blog_moment m
				LEFT JOIN blog_moment_bookmark existing ON existing.user_id = :userId AND existing.moment_id = m.id
				WHERE m.id = :momentId AND (existing.moment_id IS NOT NULL OR (
				""" + VISIBLE_MOMENT + """
				))
				ON DUPLICATE KEY UPDATE saved_at = NULL
				""").param("userId", userId).param("username", username).param("momentId", momentId).update();
	}

	public void importLegacy(Long userId, String username, List<Long> ids) {
		jdbcClient.sql("""
				INSERT INTO blog_moment_bookmark (user_id, moment_id, saved_at)
				SELECT :userId, m.id, CURRENT_TIMESTAMP(6) FROM blog_moment m
				WHERE m.id IN (:ids) AND
				""" + VISIBLE_MOMENT + """
				ORDER BY m.id
				ON DUPLICATE KEY UPDATE saved_at = blog_moment_bookmark.saved_at
				""").param("userId", userId).param("username", username).param("ids", ids).update();
	}
}
