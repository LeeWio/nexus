package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.mapper.RoleMapper;
import space.nebula.nexus.payload.request.RoleRequest;
import space.nebula.nexus.repository.MenuRepository;
import space.nebula.nexus.repository.RoleRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRoleServiceImplTest {

	@Mock private RoleRepository roleRepository;
	@Mock private MenuRepository menuRepository;
	@Mock private RoleMapper roleMapper;
	@InjectMocks private AdminRoleServiceImpl service;

	@Test
	void deleteRoleRejectsSystemRole() {
		Role adminRole = role(1L, "ROLE_ADMIN");
		when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));

		assertThrows(BusinessException.class, () -> service.deleteRole(1L));

		verify(roleRepository, never()).delete(adminRole);
	}

	@Test
	void updateRoleRejectsChangingSystemRoleCode() {
		Role userRole = role(2L, "ROLE_USER");
		when(roleRepository.findById(2L)).thenReturn(Optional.of(userRole));

		assertThrows(BusinessException.class,
				() -> service.updateRole(2L, new RoleRequest("Users", "ROLE_MEMBER", null)));

		verify(roleRepository, never()).save(userRole);
	}

	private static Role role(Long id, String code) {
		Role role = new Role();
		role.setId(id);
		role.setCode(code);
		return role;
	}
}
