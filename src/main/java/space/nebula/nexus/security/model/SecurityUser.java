package space.nebula.nexus.security.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Spring Security's representation of our custom User entity.
 */
public class SecurityUser implements UserDetails {

	private final User user;

	public SecurityUser(User user) {
		this.user = user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// Map User's Roles to Spring Security GrantedAuthorities.
		// By convention, Role names usually start with "ROLE_".
		return user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getCode()))
				.collect(Collectors.toList());
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true; // We don't have account expiration logic yet
	}

	@Override
	public boolean isAccountNonLocked() {
		// Only return false if the user is explicitly BANNED
		return user.getStatus() != UserStatus.BANNED;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		// User is only enabled if their status is ACTIVE
		return user.getStatus() == UserStatus.ACTIVE;
	}

	public User getUser() {
		return user;
	}
}
