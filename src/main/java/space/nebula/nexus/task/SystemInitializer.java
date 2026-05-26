package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.repository.UserRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

/**
 * Component to initialize system default data such as the super administrator.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemInitializer
{

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	private static final String ADMIN_USERNAME = "wei.li";
	private static final String ADMIN_EMAIL = "just.vireo@gmail.com";
	private static final String ADMIN_PASSWORD = "Wei.Li.Laba00";

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void init()
	{
		log.info("Checking system initialization...");

		// 1. Ensure ROLE_ADMIN and ROLE_USER exist
		Role adminRole = ensureRole("ROLE_ADMIN", "Super Administrator", "Has full access to all system functions");
		ensureRole("ROLE_USER", "Standard User", "Default role for registered members");

		// 2. Ensure default admin user exists
		Optional<User> adminOpt = userRepository.findByUsername(ADMIN_USERNAME);
		if (adminOpt.isEmpty())
		{
			log.info("Initializing default administrator: {}", ADMIN_USERNAME);
			User admin = new User();
			admin.setUsername(ADMIN_USERNAME);
			admin.setEmail(ADMIN_EMAIL);
			admin.setNickname("Administrator");
			admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
			admin.setStatus(UserStatus.ACTIVE);
			admin.setRoles(new HashSet<>(Collections.singletonList(adminRole)));
			userRepository.save(admin);
			log.info("Default administrator initialized successfully.");
		}
		else
		{
			// Ensure the existing user has ADMIN role and ACTIVE status
			User admin = adminOpt.get();
			boolean updated = false;
			if (admin.getStatus() != UserStatus.ACTIVE)
			{
				admin.setStatus(UserStatus.ACTIVE);
				updated = true;
			}
			if (admin.getRoles().stream().noneMatch(r -> "ROLE_ADMIN".equals(r.getCode())))
			{
				admin.getRoles().add(adminRole);
				updated = true;
			}
			if (updated)
			{
				userRepository.save(admin);
				log.info("Existing user '{}' updated to active administrator.", ADMIN_USERNAME);
			}
		}
	}

	private Role ensureRole(String code, String name, String description)
	{
		return roleRepository.findByCode(code).orElseGet(() ->
		{
			log.info("Initializing role: {}", code);
			Role role = new Role();
			role.setCode(code);
			role.setName(name);
			role.setDescription(description);
			return roleRepository.save(role);
		});
	}
}
