package space.nebula.nexus.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostLike;
import space.nebula.nexus.entity.PostLikeId;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.Tag;
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
	@Autowired
	private PostLikeRepository postLikeRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private TagRepository tagRepository;

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

	@Test
	void popularRecommendationsExcludePostsAlreadyLikedByTheReader() {
		User author = user();
		User reader = user();
		Post likedPost = post(author, "Already liked", "A previously liked post", "Content");
		postRepository.save(likedPost);

		PostLike postLike = new PostLike();
		postLike.setId(new PostLikeId(likedPost.getId(), reader.getId()));
		postLike.setPost(likedPost);
		postLike.setUser(reader);
		postLike.setCreatedAt(LocalDateTime.now());
		postLikeRepository.save(postLike);

		var recommendations = postRepository.findPopularUnseenPosts(reader.getId(), PostStatus.PUBLISHED,
				PageRequest.of(0, 10));

		assertEquals(0, recommendations.size());
	}

	@Test
	void publicFiltersCombineCategoryTagAuthorAndKeywordWithoutDuplicateRows() {
		User author = user("filter-author", "Pen Name");
		User otherAuthor = user("other-author", "Other Name");
		Category targetCategory = category("Architecture");
		Category otherCategory = category("Operations");
		Tag targetTag = tag("Java");
		Tag extraTag = tag("Spring");

		Post matching = post(author, "Distributed Systems", "A matching summary", "Content");
		matching.setCategory(targetCategory);
		matching.setTags(new java.util.HashSet<>(java.util.Set.of(targetTag, extraTag)));
		postRepository.save(matching);

		Post wrongKeyword = post(author, "Database Indexes", "A different topic", "Content");
		wrongKeyword.setCategory(targetCategory);
		wrongKeyword.setTags(new java.util.HashSet<>(java.util.Set.of(targetTag)));
		postRepository.save(wrongKeyword);

		Post wrongCategory = post(author, "Distributed Systems", "Matching title", "Content");
		wrongCategory.setCategory(otherCategory);
		wrongCategory.setTags(new java.util.HashSet<>(java.util.Set.of(targetTag)));
		postRepository.save(wrongCategory);

		Post wrongAuthor = post(otherAuthor, "Distributed Systems", "Matching title", "Content");
		wrongAuthor.setCategory(targetCategory);
		wrongAuthor.setTags(new java.util.HashSet<>(java.util.Set.of(targetTag)));
		postRepository.save(wrongAuthor);

		var page = postRepository.findAll(PostSpecification.filterPublicPosts(targetCategory.getId(), targetTag.getId(),
				" pen name ", "distributed", null, null, null), PageRequest.of(0, 1));

		assertEquals(1, page.getTotalElements());
		assertEquals(1, page.getTotalPages());
		assertEquals(matching.getId(), page.getContent().getFirst().getId());
	}

	@Test
	void publicAuthorFilterFallsBackToUsernameWhenNicknameIsBlank() {
		User author = user("blank-nickname-author", "   ");
		Post post = post(author, "Username fallback", "Summary", "Content");
		postRepository.save(post);

		var page = postRepository.findAll(PostSpecification.filterPublicPosts(null, null,
				author.getUsername().toUpperCase(), null, null, null, null), PageRequest.of(0, 10));

		assertEquals(1, page.getTotalElements());
		assertEquals(post.getId(), page.getContent().getFirst().getId());
	}

	private User user() {
		return user("post-repo-user-" + System.nanoTime(), null);
	}

	private User user(String username, String nickname) {
		User user = new User();
		user.setUsername(username + "-" + System.nanoTime());
		user.setPassword("password");
		user.setEmail(user.getUsername() + "@example.com");
		user.setNickname(nickname);
		user.setCreatedAt(LocalDateTime.now());
		return userRepository.save(user);
	}

	private Category category(String name) {
		Category category = new Category();
		category.setName(name + " " + System.nanoTime());
		category.setSlug(name.toLowerCase() + "-" + System.nanoTime());
		category.setCreatedAt(LocalDateTime.now());
		return categoryRepository.save(category);
	}

	private Tag tag(String name) {
		Tag tag = new Tag();
		tag.setName(name + " " + System.nanoTime());
		tag.setSlug(name.toLowerCase() + "-" + System.nanoTime());
		tag.setCreatedAt(LocalDateTime.now());
		return tagRepository.save(tag);
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
