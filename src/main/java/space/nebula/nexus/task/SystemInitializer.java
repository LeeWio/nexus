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
import space.nebula.nexus.config.BootstrapAdminProperties;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.constant.BusinessCode;
import cn.hutool.core.util.StrUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

/**
 * Component to initialize system default data such as the super administrator.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemInitializer {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final BootstrapAdminProperties bootstrapAdminProperties;

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void init() {
		log.info("Checking system initialization...");

		// 1. Ensure ROLE_ADMIN and ROLE_USER exist
		Role adminRole = ensureRole("ROLE_ADMIN", "Super Administrator", "Has full access to all system functions");
		ensureRole("ROLE_USER", "Standard User", "Default role for registered members");

		if (!bootstrapAdminProperties.isEnabled()) {
			return;
		}
		validateBootstrapConfiguration();

		String adminUsername = bootstrapAdminProperties.getUsername();
		String adminEmail = bootstrapAdminProperties.getEmail();
		Optional<User> adminOpt = userRepository.findByUsernameOrEmail(adminUsername, adminEmail);
		if (adminOpt.isEmpty()) {
			log.warn("Bootstrapping administrator account: {}", adminUsername);
			User admin = new User();
			admin.setUsername(adminUsername);
			admin.setEmail(adminEmail);
			admin.setNickname("Administrator");
			admin.setPassword(passwordEncoder.encode(bootstrapAdminProperties.getPassword()));
			admin.setStatus(UserStatus.ACTIVE);
			admin.setRoles(new HashSet<>(Collections.singletonList(adminRole)));
			userRepository.save(admin);
			log.info("Default administrator initialized successfully.");
		} else {
			User admin = adminOpt.get();
			boolean alreadyAdmin = admin.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getCode()));
			if (alreadyAdmin) {
				log.info("Bootstrap administrator '{}' already exists with ROLE_ADMIN", admin.getUsername());
				return;
			}

			admin.getRoles().add(adminRole);
			userRepository.save(admin);
			log.warn("Granted ROLE_ADMIN to existing bootstrap account '{}' matched by username or email",
					admin.getUsername());
		}
	}

	private void validateBootstrapConfiguration() {
		if (StrUtil.hasBlank(bootstrapAdminProperties.getUsername(), bootstrapAdminProperties.getEmail(),
				bootstrapAdminProperties.getPassword()) || bootstrapAdminProperties.getPassword().length() < 12) {
			throw new BusinessException(BusinessCode.ERROR,
					"Bootstrap administrator requires username, email and a password of at least 12 characters");
		}
	}

	private Role ensureRole(String code, String name, String description) {
		return roleRepository.findByCode(code).orElseGet(() -> {
			log.info("Initializing role: {}", code);
			Role role = new Role();
			role.setCode(code);
			role.setName(name);
			role.setDescription(description);
			return roleRepository.save(role);
		});
	}
}
