package space.nebula.nexus.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Loads a standard OAuth profile and delegates local account resolution.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
	private final OAuthAccountResolver accountResolver;

	/**
	 * Loads the provider profile and resolves it to a local Nexus principal.
	 *
	 * @param userRequest
	 *            provider user request
	 * @return local OAuth principal
	 * @throws OAuth2AuthenticationException
	 *             when profile loading or account resolution fails
	 */
	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oauth2User = super.loadUser(userRequest);
		return accountResolver.resolve(userRequest.getClientRegistration().getRegistrationId(), oauth2User);
	}
}
