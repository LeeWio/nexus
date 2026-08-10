package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PostRevision;
import space.nebula.nexus.payload.response.PostRevisionSummaryResponse;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRevisionRepository extends JpaRepository<PostRevision, Long> {

	List<PostRevision> findByPostIdOrderByVersionNumberDesc(Long postId);

	Optional<PostRevision> findFirstByPostIdOrderByVersionNumberDesc(Long postId);

	@Query("""
			SELECT new space.nebula.nexus.payload.response.PostRevisionSummaryResponse(
				revision.id,
				revision.post.id,
				revision.title,
				revision.versionNumber,
				revision.revisionKind,
				revision.changeType,
				revision.changeSummary,
				revision.baseVersionNumber,
				parent.id,
				revision.sourceRevisionId,
				revision.contentHash,
				revision.snapshotHash,
				actor.username,
				revision.createdAt)
			FROM PostRevision revision
			LEFT JOIN revision.parentRevision parent
			LEFT JOIN revision.createdBy actor
			WHERE revision.post.id = :postId
			ORDER BY revision.versionNumber DESC
			""")
	List<PostRevisionSummaryResponse> findRevisionSummariesByPostId(Long postId);

}
