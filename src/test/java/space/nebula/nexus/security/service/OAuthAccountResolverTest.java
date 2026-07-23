package space.nebula.nexus.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.repository.UserRepository;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthAccountResolverTest {
	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private OAuthAccountResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new OAuthAccountResolver(userRepository, roleRepository, passwordEncoder);
	}

	@Test
	void googleLoginCreatesLocalAccount() {
		OAuth2User oauth2User = oauthUser(Map.of("sub", "google-123", "email", "reader@example.com", "email_verified",
				true, "name", "Reader", "picture", "https://example.com/avatar.png"));
		Role role = new Role();
		role.setCode("ROLE_USER");
		when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
		when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.empty());
		when(roleRepository.findByCode("ROLE_USER")).thenReturn(Optional.of(role));
		when(passwordEncoder.encode(any())).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(10L);
			return user;
		});

		OAuth2User principal = resolver.resolve("google", oauth2User);

		assertEquals(Long.valueOf(10L), principal.<Long>getAttribute(OAuthAccountResolver.LOCAL_USER_ID_ATTRIBUTE));
		verify(userRepository).save(any(User.class));
	}

	@Test
	void githubLoginLinksExistingAccountByEmail() {
		OAuth2User oauth2User = oauthUser(Map.of("id", 99, "login", "octocat", "email", "reader@example.com",
				"avatar_url", "https://example.com/avatar.png"));
		User existing = new User();
		existing.setId(10L);
		existing.setUsername("reader");
		existing.setStatus(UserStatus.ACTIVE);
		when(userRepository.findByGithubId("99")).thenReturn(Optional.empty());
		when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(existing));

		resolver.resolve("github", oauth2User);

		assertEquals("99", existing.getGithubId());
		assertEquals("octocat", existing.getGithubUsername());
		verify(userRepository).save(existing);
	}

	@Test
	void oauthLoginRejectsUnavailableAccount() {
		OAuth2User oauth2User = oauthUser(
				Map.of("sub", "google-123", "email", "reader@example.com", "email_verified", true));
		User existing = new User();
		existing.setStatus(UserStatus.BANNED);
		when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));

		OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
				() -> resolver.resolve("google", oauth2User));

		assertEquals("oauth_account_unavailable", exception.getError().getErrorCode());
	}

	private OAuth2User oauthUser(Map<String, Object> attributes) {
		OAuth2User oauth2User = mock(OAuth2User.class);
		when(oauth2User.getAttributes()).thenReturn(attributes);
		return oauth2User;
	}
}
