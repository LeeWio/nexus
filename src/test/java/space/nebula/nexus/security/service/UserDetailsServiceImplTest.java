package space.nebula.nexus.security.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.repository.UserRepository;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserDetailsServiceImpl userDetailsService;

	@Test
	void loadUserByUsername_Success() {
		User user = new User();
		user.setUsername("testadmin");
		user.setPassword("encoded_password");
		user.setStatus(UserStatus.ACTIVE);

		Role role = new Role();
		role.setCode("ROLE_ADMIN");
		user.setRoles(Set.of(role));

		when(userRepository.findByUsernameOrEmail("testadmin", "testadmin")).thenReturn(Optional.of(user));

		UserDetails userDetails = userDetailsService.loadUserByUsername("testadmin");

		assertNotNull(userDetails);
		assertEquals("testadmin", userDetails.getUsername());
		assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
	}

	@Test
    void loadUserByUsername_NotFound() {
        when(userRepository.findByUsernameOrEmail("unknown", "unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("unknown"));
    }
}
