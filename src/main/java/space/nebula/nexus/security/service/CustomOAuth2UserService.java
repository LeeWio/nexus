package space.nebula.nexus.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.repository.UserRepository;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String clientRegistrationId = userRequest.getClientRegistration().getRegistrationId();
        
        if ("github".equals(clientRegistrationId)) {
            return processGithubUser(oauth2User);
        }
        
        return oauth2User;
    }

    private OAuth2User processGithubUser(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        String githubId = String.valueOf(attributes.get("id"));
        String username = (String) attributes.get("login");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatar = (String) attributes.get("avatar_url");
        String bio = (String) attributes.get("bio");

        Optional<User> userOptional = userRepository.findByGithubId(githubId);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Update existing user info from GitHub
            user.setGithubUsername(username);
            user.setAvatar(avatar);
            user.setNickname(name != null ? name : username);
        } else {
            // Register new user via GitHub
            user = new User();
            user.setGithubId(githubId);
            user.setGithubUsername(username);
            user.setUsername("gh_" + username); // Avoid collisions
            user.setPassword(UUID.randomUUID().toString()); // OAuth users don't use local password
            user.setEmail(email);
            user.setNickname(name != null ? name : username);
            user.setAvatar(avatar);
            user.setBio(bio);
            user.setStatus(UserStatus.ACTIVE);
            
            // Assign default USER role
            roleRepository.findByCode("ROLE_USER").ifPresent(role -> user.setRoles(Set.of(role)));
        }
        
        userRepository.save(user);

        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getCode()))
                .collect(Collectors.toSet());

        return new DefaultOAuth2User(authorities, attributes, "id");
    }
}
