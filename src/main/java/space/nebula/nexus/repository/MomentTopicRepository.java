package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.MomentTopic;

import java.util.Optional;

@Repository
public interface MomentTopicRepository extends JpaRepository<MomentTopic, Long> {
	Optional<MomentTopic> findBySlug(String slug);
}
