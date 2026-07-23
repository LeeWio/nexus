package space.nebula.nexus.security.service;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.repository.UserRepository;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves Google and GitHub profiles to local Nexus accounts in one
 * transaction.
 */
@Service
@RequiredArgsConstructor
public class OAuthAccountResolver {
	/** Attribute containing the local Nexus user identifier. */
	public static final String LOCAL_USER_ID_ATTRIBUTE = "nexus_user_id";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Resolves a provider profile to a local Nexus principal.
	 *
	 * @param registrationId
	 *            configured OAuth provider identifier
	 * @param oauth2User
	 *            provider principal
	 * @return principal enriched with the local Nexus user identifier
	 */
	@Transactional
	public OAuth2User resolve(String registrationId, OAuth2User oauth2User) {
		OAuthProfile profile = OAuthProfile.from(registrationId, oauth2User.getAttributes());
		User user = resolveUser(profile);
		validateAccountStatus(user);
		updateProfile(user, profile);
		userRepository.save(user);

		Map<String, Object> principalAttributes = new HashMap<>(oauth2User.getAttributes());
		principalAttributes.put(LOCAL_USER_ID_ATTRIBUTE, user.getId());
		Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority(role.getCode())).collect(Collectors.toSet());
		return new DefaultOAuth2User(authorities, principalAttributes, LOCAL_USER_ID_ATTRIBUTE);
	}

	private User resolveUser(OAuthProfile profile) {
		Optional<User> providerUser = profile.provider() == OAuthProvider.GITHUB
				? userRepository.findByGithubId(profile.providerUserId())
				: userRepository.findByGoogleId(profile.providerUserId());
		if (providerUser.isPresent()) {
			return providerUser.get();
		}
		if (profile.provider() == OAuthProvider.GOOGLE && profile.email() != null && !profile.emailVerified()) {
			throw oauthError("oauth_email_unverified", "Google account email is not verified");
		}

		Optional<User> emailUser = profile.email() == null
				? Optional.empty()
				: userRepository.findByEmail(profile.email());
		User user = emailUser.orElseGet(() -> createUser(profile));
		linkIdentity(user, profile);
		return user;
	}

	private User createUser(OAuthProfile profile) {
		Role defaultRole = roleRepository.findByCode("ROLE_USER")
				.orElseThrow(() -> oauthError("oauth_role_missing", "Default user role is not configured"));
		User user = new User();
		user.setUsername(generateUsername(profile));
		user.setPassword(passwordEncoder.encode(IdUtil.fastUUID()));
		user.setEmail(profile.email());
		user.setStatus(UserStatus.ACTIVE);
		user.setRoles(Set.of(defaultRole));
		return user;
	}

	private void linkIdentity(User user, OAuthProfile profile) {
		if (profile.provider() == OAuthProvider.GITHUB) {
			user.setGithubId(profile.providerUserId());
			user.setGithubUsername(profile.login());
		} else {
			user.setGoogleId(profile.providerUserId());
		}
	}

	private void updateProfile(User user, OAuthProfile profile) {
		linkIdentity(user, profile);
		if (profile.displayName() != null && !profile.displayName().isBlank()) {
			user.setNickname(profile.displayName());
		}
		if (profile.avatar() != null && !profile.avatar().isBlank()) {
			user.setAvatar(profile.avatar());
		}
		if (profile.bio() != null && !profile.bio().isBlank()) {
			user.setBio(profile.bio());
		}
		if ((user.getEmail() == null || user.getEmail().isBlank()) && profile.email() != null) {
			user.setEmail(profile.email());
		}
	}

	private void validateAccountStatus(User user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw oauthError("oauth_account_unavailable", "This account is not available for sign-in");
		}
	}

	private String generateUsername(OAuthProfile profile) {
		String emailName = profile.email() != null && profile.email().contains("@")
				? profile.email().substring(0, profile.email().indexOf('@'))
				: profile.email();
		String source = profile.login() != null ? profile.login() : emailName != null ? emailName : "user";
		String normalized = source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
		String prefix = profile.provider() == OAuthProvider.GITHUB ? "gh_" : "google_";
		String base = prefix + normalized;
		if (base.length() > 42) {
			base = base.substring(0, 42);
		}
		String candidate = base;
		int suffix = 1;
		while (userRepository.existsByUsername(candidate)) {
			candidate = base + "_" + suffix++;
		}
		return candidate;
	}

	private OAuth2AuthenticationException oauthError(String code, String message) {
		return new OAuth2AuthenticationException(new OAuth2Error(code), message);
	}

	private enum OAuthProvider {
		GITHUB, GOOGLE
	}

	private record OAuthProfile(OAuthProvider provider, String providerUserId, String login, String email,
			boolean emailVerified, String displayName, String avatar, String bio) {
		private static OAuthProfile from(String registrationId, Map<String, Object> attributes) {
			return switch (registrationId.toLowerCase(Locale.ROOT)) {
				case "github" -> new OAuthProfile(OAuthProvider.GITHUB, required(attributes, "id"),
						text(attributes, "login"), text(attributes, "email"), true, text(attributes, "name"),
						text(attributes, "avatar_url"), text(attributes, "bio"));
				case "google" -> new OAuthProfile(OAuthProvider.GOOGLE, required(attributes, "sub"), null,
						text(attributes, "email"), Boolean.parseBoolean(text(attributes, "email_verified")),
						text(attributes, "name"), text(attributes, "picture"), null);
				default -> throw new OAuth2AuthenticationException(new OAuth2Error("oauth_provider_unsupported"),
						"Unsupported OAuth provider");
			};
		}

		private static String required(Map<String, Object> attributes, String key) {
			String value = text(attributes, key);
			if (value == null || value.isBlank()) {
				throw new OAuth2AuthenticationException(new OAuth2Error("oauth_profile_invalid"),
						"OAuth provider did not return a required user identifier");
			}
			return value;
		}

		private static String text(Map<String, Object> attributes, String key) {
			Object value = attributes.get(key);
			return value == null ? null : String.valueOf(value).trim();
		}
	}
}
