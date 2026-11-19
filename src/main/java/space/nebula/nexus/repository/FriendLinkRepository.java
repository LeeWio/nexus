package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.FriendLink;
import space.nebula.nexus.enums.FriendLinkStatus;

import space.nebula.nexus.repository.projection.AssetReferenceText;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendLinkRepository extends JpaRepository<FriendLink, Long> {

	List<FriendLink> findByStatusAndIsPublishedTrueOrderBySortOrderAscCreatedAtDesc(FriendLinkStatus status);

	Optional<FriendLink> findByUrl(String url);

	@Query("""
			select new space.nebula.nexus.repository.projection.AssetReferenceText(r.id, r.name, r.avatar)
			from FriendLink r where r.avatar is not null and (:term is null or locate(:term, r.avatar) > 0)
			order by r.id
			""")
	List<AssetReferenceText> findAssetReferenceText(String term);
}
