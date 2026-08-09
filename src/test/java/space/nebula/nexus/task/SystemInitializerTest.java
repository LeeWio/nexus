package space.nebula.nexus.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import space.nebula.nexus.config.BootstrapAdminProperties;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.repository.UserRepository;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemInitializerTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private BootstrapAdminProperties properties;
	private SystemInitializer initializer;
	private Role adminRole;

	@BeforeEach
	void setUp() {
		properties = new BootstrapAdminProperties();
		properties.setEnabled(true);
		properties.setUsername("admin");
		properties.setEmail("admin@example.com");
		properties.setPassword("a-secure-bootstrap-password");
		initializer = new SystemInitializer(userRepository, roleRepository, passwordEncoder, properties);

		adminRole = new Role();
		adminRole.setCode("ROLE_ADMIN");
		when(roleRepository.findByCode("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
		when(roleRepository.findByCode("ROLE_USER")).thenReturn(Optional.of(new Role()));
	}

	@Test
	void reusesAccountMatchedByEmailAndGrantsAdminRole() {
		User existing = new User();
		existing.setUsername("existing-user");
		existing.setEmail("admin@example.com");
		existing.setRoles(new HashSet<>());
		when(userRepository.findByUsernameOrEmail("admin", "admin@example.com")).thenReturn(Optional.of(existing));

		initializer.init();

		assertTrue(existing.getRoles().contains(adminRole));
		verify(userRepository).save(existing);
		verify(passwordEncoder, never()).encode(properties.getPassword());
	}

	@Test
	void leavesExistingAdministratorUnchanged() {
		User existing = new User();
		existing.setUsername("admin");
		existing.setEmail("admin@example.com");
		existing.setRoles(new HashSet<>());
		existing.getRoles().add(adminRole);
		when(userRepository.findByUsernameOrEmail("admin", "admin@example.com")).thenReturn(Optional.of(existing));

		initializer.init();

		verify(userRepository, never()).save(existing);
		verify(passwordEncoder, never()).encode(properties.getPassword());
	}
}
