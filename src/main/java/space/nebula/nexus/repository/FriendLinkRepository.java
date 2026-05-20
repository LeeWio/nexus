package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.FriendLink;
import space.nebula.nexus.enums.FriendLinkStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendLinkRepository extends JpaRepository<FriendLink, Long> {

	List<FriendLink> findByStatusAndIsPublishedTrueOrderBySortOrderAscCreatedAtDesc(FriendLinkStatus status);

	Optional<FriendLink> findByUrl(String url);
}
