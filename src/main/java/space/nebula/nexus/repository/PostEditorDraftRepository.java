package space.nebula.nexus.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import space.nebula.nexus.entity.PostEditorDraft;

import java.util.Optional;

public interface PostEditorDraftRepository extends JpaRepository<PostEditorDraft, Long> {
	Optional<PostEditorDraft> findByUserIdAndIdentifier(Long userId, String identifier);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from PostEditorDraft d where d.userId = :userId and d.identifier = :identifier")
	Optional<PostEditorDraft> findForUpdate(Long userId, String identifier);

	@Query("""
			select d.id from PostEditorDraft d
			where locate(:name, cast(d.content as String)) > 0 or locate(:name, cast(d.postData as String)) > 0
			or (:thumbnail is not null and (locate(:thumbnail, cast(d.content as String)) > 0
				or locate(:thumbnail, cast(d.postData as String)) > 0))
			order by d.id
			""")
	java.util.List<Long> findAssetReferences(String name, String thumbnail, Limit limit);
}
