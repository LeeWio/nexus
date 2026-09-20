package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import space.nebula.nexus.entity.MomentXSync;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MomentXSyncRepository extends JpaRepository<MomentXSync, Long> {

	Optional<MomentXSync> findByMomentId(Long momentId);

	@Query("select s from MomentXSync s join fetch s.moment m where m.id in :momentIds")
	List<MomentXSync> findByMomentIdIn(@Param("momentIds") Collection<Long> momentIds);
}
