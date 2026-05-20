package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

	/**
	 * Finds a role by its unique code (e.g., "ROLE_USER").
	 */
	@EntityGraph(attributePaths = {"menus"})
	Optional<Role> findByCode(String code);

	@Override
	@EntityGraph(attributePaths = {"menus"})
	List<Role> findAll();
}
