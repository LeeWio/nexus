package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Subscriber;
import space.nebula.nexus.enums.SubscriberStatus;

import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
	Optional<Subscriber> findByEmail(String email);
	Optional<Subscriber> findByVerificationToken(String token);
	Optional<Subscriber> findByUnsubscribeToken(String token);
	Page<Subscriber> findAllByStatus(SubscriberStatus status, Pageable pageable);
}
