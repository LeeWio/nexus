package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PostRevision;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRevisionRepository extends JpaRepository<PostRevision, Long> {

	List<PostRevision> findByPostIdOrderByVersionNumberDesc(Long postId);

	@Query("SELECT MAX(pr.versionNumber) FROM PostRevision pr WHERE pr.post.id = :postId")
	Optional<Integer> findMaxVersionByPostId(Long postId);
}
