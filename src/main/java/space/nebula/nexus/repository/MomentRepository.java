package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.enums.MomentVisibility;

import java.util.Collection;
import java.util.List;

@Repository
public interface MomentRepository extends JpaRepository<Moment, Long> {
	@Query("SELECT m FROM Moment m WHERE m.visibility = :publicVisibility "
			+ "OR (:username IS NOT NULL AND m.createdBy = :username) " + "ORDER BY m.createdAt DESC")
	Page<Moment> findPublicTimeline(@Param("publicVisibility") MomentVisibility publicVisibility,
			@Param("username") String username, Pageable pageable);

	@Query("SELECT m FROM Moment m JOIN m.topicRelations relation JOIN relation.topic topic "
			+ "WHERE topic.slug = :slug AND (m.visibility = :publicVisibility "
			+ "OR (:username IS NOT NULL AND m.createdBy = :username)) ORDER BY m.createdAt DESC")
	Page<Moment> findPublicTimelineByTopic(@Param("slug") String slug,
			@Param("publicVisibility") MomentVisibility publicVisibility, @Param("username") String username,
			Pageable pageable);

	@Query("SELECT topic.slug, COUNT(moment.id) FROM Moment moment "
			+ "JOIN moment.topicRelations relation JOIN relation.topic topic "
			+ "WHERE moment.visibility = :visibility GROUP BY topic.slug "
			+ "ORDER BY COUNT(moment.id) DESC, topic.slug ASC")
	List<Object[]> countPublicTopics(@Param("visibility") MomentVisibility visibility);

	List<Moment> findByContentContainingIgnoreCaseAndVisibility(String content, MomentVisibility visibility);

	List<Moment> findTop10ByOrderByCreatedAtDesc();

	List<Moment> findByCreatedAtBetweenOrderByCreatedAtAsc(java.time.LocalDateTime from, java.time.LocalDateTime to);

	/**
	 * Returns publication times for public moments inside a stored UTC range.
	 * 
	 * @param visibility
	 *            visibility included in the cadence
	 * @param from
	 *            inclusive stored publication time
	 * @param to
	 *            exclusive stored publication time
	 * @return publication times in ascending order
	 */
	@Query("SELECT m.createdAt FROM Moment m WHERE m.visibility = :visibility "
			+ "AND m.createdAt >= :from AND m.createdAt < :to ORDER BY m.createdAt ASC")
	List<java.time.LocalDateTime> findPublicCreatedAtBetween(@Param("visibility") MomentVisibility visibility,
			@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

	@Modifying
	@Query("update Moment moment set moment.likesCount = case when coalesce(moment.likesCount, 0) + :delta < 0 then 0 else coalesce(moment.likesCount, 0) + :delta end where moment.id = :id")
	void incrementLikes(@Param("id") Long id, @Param("delta") Long delta);

	@Query(value = "select moment_id from blog_moment_like where user_id = :userId and moment_id in (:momentIds)", nativeQuery = true)
	List<Long> findLikedMomentIdsByUserIdAndMomentIdIn(@Param("userId") Long userId,
			@Param("momentIds") Collection<Long> momentIds);
}
