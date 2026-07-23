package space.nebula.nexus.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class CommentRepositoryDataJpaTest {

	@Autowired
	private CommentRepository commentRepository;
	@Autowired
	private PostRepository postRepository;
	@Autowired
	private UserRepository userRepository;

	@Test
	void hotRootCommentsPrioritizePinnedFeaturedAndLikes() {
		User user = user();
		Post post = post(user);
		Comment plain = comment(post, user, 10L, false, false);
		Comment featured = comment(post, user, 1L, false, true);
		Comment pinned = comment(post, user, 0L, true, false);
		commentRepository.save(plain);
		commentRepository.save(featured);
		commentRepository.save(pinned);

		var page = commentRepository.findHotRootCommentsByPost(post.getId(), CommentStatus.APPROVED,
				PageRequest.of(0, 10));

		assertEquals(pinned.getId(), page.getContent().get(0).getId());
		assertEquals(featured.getId(), page.getContent().get(1).getId());
		assertEquals(plain.getId(), page.getContent().get(2).getId());
	}

	@Test
	void newRootCommentsUseForwardAnchor() {
		User user = user();
		Post post = post(user);
		Comment first = comment(post, user, 0L, false, false);
		Comment second = comment(post, user, 0L, false, false);
		commentRepository.save(first);
		commentRepository.save(second);

		var comments = commentRepository.findNewRootCommentsByPost(post.getId(), CommentStatus.APPROVED, first.getId(),
				PageRequest.of(0, 10));

		assertEquals(1, comments.size());
		assertEquals(second.getId(), comments.getFirst().getId());
	}

	private User user() {
		User user = new User();
		user.setUsername("repo-user-" + System.nanoTime());
		user.setPassword("password");
		user.setEmail(user.getUsername() + "@example.com");
		user.setCreatedAt(LocalDateTime.now());
		return userRepository.save(user);
	}

	private Post post(User author) {
		Post post = new Post();
		post.setTitle("Repository Test Post " + System.nanoTime());
		post.setSlug("repository-test-post-" + System.nanoTime());
		post.setContent("{}");
		post.setContentType(PostContentType.JSON);
		post.setStatus(PostStatus.PUBLISHED);
		post.setAuthor(author);
		post.setCreatedAt(LocalDateTime.now());
		return postRepository.save(post);
	}

	private Comment comment(Post post, User user, Long likes, boolean pinned, boolean featured) {
		Comment comment = new Comment();
		comment.setContent("comment");
		comment.setPost(post);
		comment.setUser(user);
		comment.setStatus(CommentStatus.APPROVED);
		comment.setLikesCount(likes);
		comment.setPinned(pinned);
		comment.setFeatured(featured);
		comment.setCreatedAt(LocalDateTime.now());
		return comment;
	}
}
