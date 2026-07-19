package space.nebula.nexus.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Adapts Google OpenID Connect identities to the shared OAuth account resolver.
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService
{
	private final OAuthAccountResolver accountResolver;

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException
	{
		OidcUser oidcUser = super.loadUser(userRequest);
		OAuth2User localPrincipal = accountResolver.resolve(
				userRequest.getClientRegistration().getRegistrationId(), oidcUser);
		return new LocalOidcUser(oidcUser, localPrincipal);
	}
}
