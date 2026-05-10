package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Moment;

@Repository
public interface MomentRepository extends JpaRepository<Moment, Long> {
    Page<Moment> findByIsPublishedTrueOrderByCreatedAtDesc(Pageable pageable);
}
