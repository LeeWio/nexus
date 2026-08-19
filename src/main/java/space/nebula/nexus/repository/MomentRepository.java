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

	List<Moment> findByContentContainingIgnoreCaseAndVisibility(String content, MomentVisibility visibility);

	@Modifying
	@Query("update Moment moment set moment.likesCount = case when coalesce(moment.likesCount, 0) + :delta < 0 then 0 else coalesce(moment.likesCount, 0) + :delta end where moment.id = :id")
	void incrementLikes(@Param("id") Long id, @Param("delta") Long delta);

	@Query(value = "select moment_id from blog_moment_like where user_id = :userId and moment_id in (:momentIds)", nativeQuery = true)
	List<Long> findLikedMomentIdsByUserIdAndMomentIdIn(@Param("userId") Long userId,
			@Param("momentIds") Collection<Long> momentIds);
}
