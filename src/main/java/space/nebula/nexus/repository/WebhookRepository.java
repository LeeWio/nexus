package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Webhook;

import java.util.List;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {
	List<Webhook> findAllByIsActiveTrue();
}
