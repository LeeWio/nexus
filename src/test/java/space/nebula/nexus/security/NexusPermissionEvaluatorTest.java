package space.nebula.nexus.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.security.model.SecurityUser;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NexusPermissionEvaluatorTest {
	@Mock
	private PostRepository postRepository;

	@InjectMocks
	private NexusPermissionEvaluator permissionEvaluator;

	@Test
	void deniesReadAccessToAnotherAuthorsPost() {
		Post post = post(41L, user(1L, "author"));
		when(postRepository.findById(41L)).thenReturn(Optional.of(post));

		boolean allowed = permissionEvaluator.hasPermission(authentication(user(2L, "reader"), "ROLE_USER"), 41L,
				"Post", "READ");

		assertFalse(allowed);
	}

	@Test
	void allowsAuthorsAndEditorsToReadPostManagementData() {
		User author = user(1L, "author");
		Post post = post(42L, author);
		when(postRepository.findById(42L)).thenReturn(Optional.of(post));

		assertTrue(permissionEvaluator.hasPermission(authentication(author, "ROLE_USER"), 42L, "Post", "READ"));
		assertTrue(permissionEvaluator.hasPermission(authentication(user(3L, "editor"), "ROLE_EDITOR"), 42L, "Post",
				"READ"));
	}

	private Authentication authentication(User user, String role) {
		return new UsernamePasswordAuthenticationToken(new SecurityUser(user), "credentials",
				List.of(new SimpleGrantedAuthority(role)));
	}

	private Post post(Long id, User author) {
		Post post = new Post();
		post.setId(id);
		post.setAuthor(author);
		return post;
	}

	private User user(Long id, String username) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		return user;
	}
}
