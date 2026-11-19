package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.Project;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.MomentVisibility;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.ProjectRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.UserRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class DatabasePostSearchServiceDataJpaTest {

	@Autowired
	private PostRepository postRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private TagRepository tagRepository;
	@Autowired
	private ProjectRepository projectRepository;
	@Autowired
	private MomentRepository momentRepository;
	@Autowired
	private UserRepository userRepository;

	private DatabasePostSearchServiceImpl searchService;

	@BeforeEach
	void setUp() {
		searchService = new DatabasePostSearchServiceImpl(postRepository, categoryRepository, tagRepository,
				projectRepository, momentRepository);
	}

	@Test
	void unifiedSearchMatchesPostLobContentWithoutLowerFunction() {
		User author = user();
		Post post = post(author, "Unified Search", "No matching summary",
				"Production log analysis points to LowerOnClobRegression in the article body.");
		postRepository.save(post);

		var response = searchService.unifiedSearch("LowerOnClobRegression");

		assertEquals(1, response.data().getTotalHits());
		var result = response.data().getGroups().getFirst().getItems().getFirst();
		assertEquals("post:" + post.getId(), result.getId());
		assertEquals("/single/" + post.getSlug(), result.getUrl());
		assertEquals(post.getTitle(), result.getTitle());
		assertTrue(result.getDescription().contains("LowerOnClobRegression"));
		assertFalse(result.getDescription().contains("<mark"));
	}

	@Test
	void unifiedSearchMatchesMomentTextContentIgnoringCase() {
		Moment moment = new Moment();
		moment.setContent("A moment about MomentTextCaseRegression.");
		moment.setVisibility(MomentVisibility.PUBLIC);
		moment.setCreatedAt(LocalDateTime.now());
		momentRepository.save(moment);

		var response = searchService.unifiedSearch("momenttextcaseregression");

		assertEquals(1, response.data().getTotalHits());
		var result = response.data().getGroups().getFirst().getItems().getFirst();
		assertEquals("moment:" + moment.getId(), result.getId());
		assertEquals("/moments#moment-" + moment.getId(), result.getUrl());
	}

	@Test
	void unifiedSearchLimitsProjectsToPublishedMatchesAndEncodesProjectFilter() {
		saveProject("Private project route example", "private-route", null, false);
		saveProject("Public project", "连接 & 读者", "A project route example", true);

		var response = searchService.unifiedSearch("project route example");

		assertEquals(1, response.data().getTotalHits());
		var result = response.data().getGroups().getFirst().getItems().getFirst();
		assertEquals("Public project", result.getTitle());
		assertEquals("/projects?q=%E8%BF%9E%E6%8E%A5%20%26%20%E8%AF%BB%E8%80%85", result.getUrl());
	}

	private void saveProject(String name, String slug, String description, boolean published) {
		Project project = new Project();
		project.setName(name);
		project.setSlug(slug);
		project.setDescription(description);
		project.setIsPublished(published);
		project.setCreatedAt(LocalDateTime.now());
		projectRepository.save(project);
	}

	private User user() {
		User user = new User();
		user.setUsername("db-search-user-" + System.nanoTime());
		user.setPassword("password");
		user.setEmail(user.getUsername() + "@example.com");
		user.setCreatedAt(LocalDateTime.now());
		return userRepository.save(user);
	}

	private Post post(User author, String title, String summary, String content) {
		Post post = new Post();
		post.setTitle(title + " " + System.nanoTime());
		post.setSlug("database-search-test-" + System.nanoTime());
		post.setSummary(summary);
		post.setContent(content);
		post.setContentType(PostContentType.MDX);
		post.setStatus(PostStatus.PUBLISHED);
		post.setAuthor(author);
		post.setCreatedAt(LocalDateTime.now());
		return post;
	}
}
