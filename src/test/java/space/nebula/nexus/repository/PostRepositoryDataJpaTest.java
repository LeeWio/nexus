package space.nebula.nexus.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.repository.specification.PostSpecification;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class PostRepositoryDataJpaTest {

	@Autowired
	private PostRepository postRepository;
	@Autowired
	private UserRepository userRepository;

	@Test
	void publicKeywordSearchMatchesLobContentWithoutLowerFunction() {
		User author = user();
		Post post = post(author, "Professional Search", "No matching summary",
				"Deep observability notes mention CursorAnchorWindow in the article body.");
		postRepository.save(post);

		var page = postRepository.findAll(
				PostSpecification.filterPublicPosts(null, null, "CursorAnchorWindow", null, null, null),
				PageRequest.of(0, 10));

		assertEquals(1, page.getTotalElements());
		assertEquals(post.getId(), page.getContent().getFirst().getId());
	}

	private User user() {
		User user = new User();
		user.setUsername("post-repo-user-" + System.nanoTime());
		user.setPassword("password");
		user.setEmail(user.getUsername() + "@example.com");
		user.setCreatedAt(LocalDateTime.now());
		return userRepository.save(user);
	}

	private Post post(User author, String title, String summary, String content) {
		Post post = new Post();
		post.setTitle(title + " " + System.nanoTime());
		post.setSlug("post-repository-test-" + System.nanoTime());
		post.setSummary(summary);
		post.setContent(content);
		post.setContentType(PostContentType.MDX);
		post.setStatus(PostStatus.PUBLISHED);
		post.setAuthor(author);
		post.setCreatedAt(LocalDateTime.now());
		return post;
	}
}
