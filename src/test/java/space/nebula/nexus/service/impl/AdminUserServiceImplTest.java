package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.mapper.UserMapper;
import space.nebula.nexus.payload.request.AssignRoleRequest;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private RoleRepository roleRepository;
	@Mock
	private UserMapper userMapper;
	@InjectMocks
	private AdminUserServiceImpl service;

	@Test
	void disableUserRejectsRemovingLastActiveAdministrator() {
		User admin = activeAdmin(1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(userRepository.findByRoleCodeAndStatusForUpdate("ROLE_ADMIN", UserStatus.ACTIVE))
				.thenReturn(List.of(admin));

		assertThrows(BusinessException.class, () -> service.disableUser(1L));

		verify(userRepository, never()).save(admin);
	}

	@Test
	void disableUserAllowsRemovalWhenAnotherActiveAdministratorExists() {
		User target = activeAdmin(1L);
		User remaining = activeAdmin(2L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(target));
		when(userRepository.findByRoleCodeAndStatusForUpdate("ROLE_ADMIN", UserStatus.ACTIVE))
				.thenReturn(List.of(target, remaining));

		service.disableUser(1L);

		verify(userRepository).save(target);
	}

	@Test
	void assignRolesRejectsRemovingLastAdministratorRole() {
		User admin = activeAdmin(1L);
		Role userRole = role(2L, "ROLE_USER");
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(roleRepository.findAllById(List.of(2L))).thenReturn(List.of(userRole));
		when(userRepository.findByRoleCodeAndStatusForUpdate("ROLE_ADMIN", UserStatus.ACTIVE))
				.thenReturn(List.of(admin));

		assertThrows(BusinessException.class, () -> service.assignRoles(1L, new AssignRoleRequest(List.of(2L))));

		verify(userRepository, never()).save(admin);
	}

	private static User activeAdmin(Long id) {
		User user = new User();
		user.setId(id);
		user.setStatus(UserStatus.ACTIVE);
		user.setRoles(Set.of(role(1L, "ROLE_ADMIN")));
		return user;
	}

	private static Role role(Long id, String code) {
		Role role = new Role();
		role.setId(id);
		role.setCode(code);
		return role;
	}
}
