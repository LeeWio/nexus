package space.nebula.nexus.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PostRevision;
import space.nebula.nexus.payload.response.PostRevisionSummaryResponse;

import space.nebula.nexus.repository.projection.RevisionAssetReference;

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

	@Query("""
			SELECT new space.nebula.nexus.repository.projection.RevisionAssetReference(
				revision.id, revision.title, revision.versionNumber)
			FROM PostRevision revision JOIN revision.post post
			WHERE post.isDeleted = false AND (locate(:name, cast(revision.content as String)) > 0
				OR locate(:name, revision.summary) > 0
				OR locate(:name, cast(revision.snapshotJson as String)) > 0
				OR (:thumbnail IS NOT NULL AND (
					locate(:thumbnail, cast(revision.content as String)) > 0
					OR locate(:thumbnail, revision.summary) > 0
					OR locate(:thumbnail, cast(revision.snapshotJson as String)) > 0)))
			ORDER BY revision.id
			""")
	List<RevisionAssetReference> findAssetReferences(String name, String thumbnail, Limit limit);
}
