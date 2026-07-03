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
	 * Finds a user by their username. Uses EntityGraph to eagerly fetch the roles
	 * to avoid LazyInitializationException during authentication.
	 */
	@EntityGraph(attributePaths = "roles")
	Optional<User> findByUsername(String username);

	/**
	 * Finds a user by their username or email.
	 */
	@EntityGraph(attributePaths = "roles")
	Optional<User> findByUsernameOrEmail(String username, String email);

	@EntityGraph(attributePaths = "roles")
	Optional<User> findByEmail(String email);

	@Override
	@EntityGraph(attributePaths = "roles")
	Page<User> findAll(Pageable pageable);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	@org.springframework.data.jpa.repository.Query("SELECT u.avatar FROM User u WHERE u.avatar IS NOT NULL")
	java.util.List<String> findAllAvatars();

	java.util.Optional<User> findByGithubId(String githubId);

	boolean existsByAvatarContaining(String keyword);
}
