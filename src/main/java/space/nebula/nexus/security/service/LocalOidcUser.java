package space.nebula.nexus.security.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * OIDC principal that preserves Google claims while exposing Nexus authorities
 * and local user ID.
 */
public final class LocalOidcUser implements OidcUser {
	private final OidcUser oidcUser;
	private final OAuth2User localPrincipal;

	/**
	 * Creates a combined Google and Nexus principal.
	 *
	 * @param oidcUser
	 *            original Google OIDC principal
	 * @param localPrincipal
	 *            resolved Nexus principal
	 */
	public LocalOidcUser(OidcUser oidcUser, OAuth2User localPrincipal) {
		this.oidcUser = oidcUser;
		this.localPrincipal = localPrincipal;
	}

	@Override
	public Map<String, Object> getClaims() {
		return localPrincipal.getAttributes();
	}

	@Override
	public OidcUserInfo getUserInfo() {
		return oidcUser.getUserInfo();
	}

	@Override
	public OidcIdToken getIdToken() {
		return oidcUser.getIdToken();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return localPrincipal.getAuthorities();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return localPrincipal.getAttributes();
	}

	@Override
	public String getName() {
		return localPrincipal.getName();
	}
}
