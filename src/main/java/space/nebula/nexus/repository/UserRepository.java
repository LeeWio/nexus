package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their username.
     * Uses EntityGraph to eagerly fetch the roles to avoid LazyInitializationException during authentication.
     */
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = "roles")
    Page<User> findAll(Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    java.util.Optional<User> findByGithubId(String githubId);
}
