package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import space.nebula.nexus.enums.UserStatus;

import java.util.List;
import space.nebula.nexus.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * Locks all active users assigned to a role so administrative invariants can be
	 * checked without concurrent removal races.
	 *
	 * @param roleCode
	 *            the role code
	 * @param status
	 *            the required account status
	 * @return locked users assigned to the role
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "roles")
	@Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.code = :roleCode AND u.status = :status")
	List<User> findByRoleCodeAndStatusForUpdate(String roleCode, UserStatus status);

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

	java.util.Optional<User> findByGoogleId(String googleId);

	boolean existsByAvatarContaining(String keyword);
}
