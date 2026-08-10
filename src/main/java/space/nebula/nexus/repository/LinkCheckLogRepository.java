package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.LinkCheckLog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LinkCheckLogRepository extends JpaRepository<LinkCheckLog, Long> {
	Optional<LinkCheckLog> findByUrlAndSourceTypeAndSourceId(String url, String sourceType, Long sourceId);
	List<LinkCheckLog> findBySourceTypeAndSourceIdIn(String sourceType, Collection<Long> sourceIds);
	Page<LinkCheckLog> findByIsBrokenTrue(Pageable pageable);
}
