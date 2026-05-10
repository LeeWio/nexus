package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.FriendLink;

import java.util.List;

@Repository
public interface FriendLinkRepository extends JpaRepository<FriendLink, Long> {
    
    List<FriendLink> findByIsPublishedTrueOrderBySortOrderAscCreatedAtDesc();
}
