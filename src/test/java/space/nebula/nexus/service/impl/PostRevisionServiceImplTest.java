package space.nebula.nexus.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostRevision;
import space.nebula.nexus.entity.PostSeries;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostRevisionKind;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.mapper.PostRevisionMapper;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.PostRevisionSnapshot;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.PostRevisionRepository;
import space.nebula.nexus.repository.PostSeriesRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostRevisionServiceImplTest {

	@Mock
	private PostRevisionRepository postRevisionRepository;
	@Mock
	private PostRevisionMapper postRevisionMapper;
	@Mock
	private PostRepository postRepository;
	@Mock
	private PostMapper postMapper;
	@Mock
	private UserRepository userRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private TagRepository tagRepository;
	@Mock
	private PostSeriesRepository seriesRepository;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	private final ObjectMapper objectMapper = new ObjectMapper();
	@InjectMocks
	private PostRevisionServiceImpl postRevisionService;

	@Test
	@DisplayName("Should capture a complete immutable snapshot with the actual editor")
	void saveRevision_CapturesSnapshotAndActor() throws Exception {
		Post post = post(11L, "Original title", "Original content");
		post.setCoverImage("https://cdn.example.com/cover.png");
		User editor = user(7L, "editor");
		when(postRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(post));
		when(postRevisionRepository.findFirstByPostIdOrderByVersionNumberDesc(11L)).thenReturn(Optional.empty());
		when(postRevisionRepository.save(any(PostRevision.class))).thenAnswer(invocation -> {
			PostRevision revision = invocation.getArgument(0);
			revision.setId(101L);
			return revision;
		});

		try (MockedStatic<SecurityUtil> security = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
			security.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(editor);

			postRevisionService.saveRevision(post, PostRevisionKind.CREATED, "Initial post content");
		}

		ArgumentCaptor<PostRevision> captor = ArgumentCaptor.forClass(PostRevision.class);
		verify(postRevisionRepository).save(captor.capture());
		PostRevision saved = captor.getValue();
		PostRevisionSnapshot snapshot = objectMapper.readValue(saved.getSnapshotJson(), PostRevisionSnapshot.class);
		assertEquals(1, saved.getVersionNumber());
		assertEquals(PostRevisionKind.CREATED, saved.getRevisionKind());
		assertSame(editor, saved.getCreatedBy());
		assertEquals(post.getTitle(), snapshot.title());
		assertEquals(post.getCoverImage(), snapshot.coverImage());
		assertEquals(post.getContent(), snapshot.content());
		assertTrue(saved.getSnapshotHash().matches("[0-9a-f]{64}"));
	}

	@Test
	@DisplayName("Should reject a write based on a stale revision number")
	void assertExpectedRevision_RejectsStaleVersion() {
		PostRevision latest = new PostRevision();
		latest.setVersionNumber(4);
		when(postRevisionRepository.findFirstByPostIdOrderByVersionNumberDesc(12L)).thenReturn(Optional.of(latest));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> postRevisionService.assertExpectedRevision(12L, 3));

		assertEquals(409, exception.getCode());
	}

	@Test
	@DisplayName("Should restore a snapshot by appending a new revision")
	void revertToRevision_AppendsRestorationRevision() throws Exception {
		Post post = post(13L, "Current title", "Current content");
		PostRevision target = new PostRevision();
		target.setId(201L);
		target.setPost(post);
		target.setVersionNumber(1);
		target.setSnapshotJson(objectMapper.writeValueAsString(
				new PostRevisionSnapshot("Restored title", "restored-title", "cover.png", "Restored summary",
						"Restored content", PostContentType.MDX, PostStatus.DRAFT, false, null, java.util.Set.of(),
						null, 0, null)));
		PostRevision latest = new PostRevision();
		latest.setId(202L);
		latest.setVersionNumber(2);
		latest.setSnapshotHash("existing-hash");
		User editor = user(8L, "editor");
		PostResponse response = org.mockito.Mockito.mock(PostResponse.class);

		when(postRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(post));
		when(postRevisionRepository.findById(201L)).thenReturn(Optional.of(target));
		when(postRevisionRepository.findFirstByPostIdOrderByVersionNumberDesc(13L)).thenReturn(Optional.of(latest));
		when(postRevisionRepository.save(any(PostRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(postMapper.toResponse(post)).thenReturn(response);

		try (MockedStatic<SecurityUtil> security = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
			security.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(editor);

			ApiResponse<PostResponse> result = postRevisionService.revertToRevision(13L, 201L, 2);

			assertSame(response, result.data());
		}

		ArgumentCaptor<PostRevision> captor = ArgumentCaptor.forClass(PostRevision.class);
		verify(postRevisionRepository).save(captor.capture());
		PostRevision restoration = captor.getValue();
		assertEquals("Restored title", post.getTitle());
		assertEquals("Restored content", post.getContent());
		assertEquals(PostRevisionKind.RESTORED, restoration.getRevisionKind());
		assertEquals(201L, restoration.getSourceRevisionId());
		assertEquals(3, restoration.getVersionNumber());
		verify(eventPublisher).publishEvent(any());
	}

	@Test
	@DisplayName("Should restore editable metadata while preserving the current workflow state")
	void revertToRevision_RestoresEditableMetadataAndPreservesWorkflowState() throws Exception {
		Post post = post(15L, "Current title", "Current content");
		post.setSlug("current-slug");
		post.setCoverImage("current-cover.png");
		post.setIsFeatured(false);
		post.setStatus(PostStatus.PUBLISHED);
		post.setPath("/15/");

		Category category = new Category();
		category.setId(101L);
		Tag firstTag = tag(201L, "first-tag");
		Tag secondTag = tag(202L, "second-tag");
		PostSeries series = new PostSeries();
		series.setId(301L);
		Post parent = post(401L, "Parent", "Parent content");
		parent.setPath("/401/");

		PostRevision target = new PostRevision();
		target.setId(401L);
		target.setPost(post);
		target.setVersionNumber(1);
		target.setSnapshotJson(objectMapper.writeValueAsString(new PostRevisionSnapshot("Restored title",
				"restored-slug", "restored-cover.png", "Restored summary", "Restored content", PostContentType.MDX,
				PostStatus.DRAFT, true, 101L, Set.of(201L, 202L), 301L, 7, 401L)));
		PostRevision latest = latestRevision(2, "existing-hash");
		User editor = user(8L, "editor");

		when(postRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(post));
		when(postRevisionRepository.findById(401L)).thenReturn(Optional.of(target));
		when(postRevisionRepository.findFirstByPostIdOrderByVersionNumberDesc(15L)).thenReturn(Optional.of(latest));
		when(postRepository.findBySlug("restored-slug")).thenReturn(Optional.empty());
		when(categoryRepository.findById(101L)).thenReturn(Optional.of(category));
		when(tagRepository.findAllById(Set.of(201L, 202L))).thenReturn(List.of(firstTag, secondTag));
		when(seriesRepository.findById(301L)).thenReturn(Optional.of(series));
		when(postRepository.findById(401L)).thenReturn(Optional.of(parent));
		when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(postRevisionRepository.save(any(PostRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(postMapper.toResponse(post)).thenReturn(org.mockito.Mockito.mock(PostResponse.class));

		try (MockedStatic<SecurityUtil> security = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
			security.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(editor);

			postRevisionService.revertToRevision(15L, 401L, 2);
		}

		assertEquals("restored-slug", post.getSlug());
		assertEquals("restored-cover.png", post.getCoverImage());
		assertEquals(category, post.getCategory());
		assertEquals(Set.of(firstTag, secondTag), post.getTags());
		assertEquals(series, post.getSeries());
		assertEquals(7, post.getSeriesOrder());
		assertEquals(parent, post.getParent());
		assertEquals("/401/15/", post.getPath());
		assertEquals(PostStatus.PUBLISHED, post.getStatus());
		assertEquals(true, post.getIsFeatured());
		verify(postRepository).replaceDescendantPathPrefix(15L, "/15/", "/401/15/");
	}

	@Test
	@DisplayName("Should reject restoring a revision with deleted tags")
	void revertToRevision_RejectsUnavailableTags() throws Exception {
		Post post = post(16L, "Current title", "Current content");
		PostRevision target = new PostRevision();
		target.setId(501L);
		target.setPost(post);
		target.setVersionNumber(1);
		target.setSnapshotJson(objectMapper.writeValueAsString(new PostRevisionSnapshot("Restored title",
				"restored-slug", null, "Restored summary", "Restored content", PostContentType.MDX,
				PostStatus.DRAFT, false, null, Set.of(701L), null, 0, null)));

		when(postRepository.findByIdForUpdate(16L)).thenReturn(Optional.of(post));
		when(postRevisionRepository.findById(501L)).thenReturn(Optional.of(target));
		when(postRevisionRepository.findFirstByPostIdOrderByVersionNumberDesc(16L))
				.thenReturn(Optional.of(latestRevision(2, "existing-hash")));
		when(postRepository.findBySlug("restored-slug")).thenReturn(Optional.empty());
		when(tagRepository.findAllById(Set.of(701L))).thenReturn(List.of());

		BusinessException exception = assertThrows(BusinessException.class,
				() -> postRevisionService.revertToRevision(16L, 501L, 2));

		assertEquals(BusinessCode.BAD_REQUEST.getCode(), exception.getCode());
		verify(postRepository, never()).save(any(Post.class));
	}

	@Test
	@DisplayName("Should preserve metadata missing from a legacy revision during restoration")
	void revertToLegacyRevision_PreservesMissingMetadata() {
		Post post = post(14L, "Current title", "Current content");
		post.setCoverImage("current-cover.png");
		PostRevision target = new PostRevision();
		target.setId(301L);
		target.setPost(post);
		target.setVersionNumber(1);
		target.setTitle("Legacy title");
		target.setSummary("Legacy summary");
		target.setContent("Legacy content");
		target.setContentType(PostContentType.MDX);
		PostRevision latest = new PostRevision();
		latest.setId(302L);
		latest.setVersionNumber(2);
		latest.setSnapshotHash("existing-hash");
		User editor = user(9L, "editor");

		when(postRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(post));
		when(postRevisionRepository.findById(301L)).thenReturn(Optional.of(target));
		when(postRevisionRepository.findFirstByPostIdOrderByVersionNumberDesc(14L)).thenReturn(Optional.of(latest));
		when(postRevisionRepository.save(any(PostRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(postMapper.toResponse(post)).thenReturn(org.mockito.Mockito.mock(PostResponse.class));

		try (MockedStatic<SecurityUtil> security = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
			security.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(editor);

			postRevisionService.revertToRevision(14L, 301L, 2);
		}

		assertEquals("Legacy title", post.getTitle());
		assertEquals("Legacy content", post.getContent());
		assertEquals("current-cover.png", post.getCoverImage());
	}

	private Post post(Long id, String title, String content) {
		Post post = new Post();
		post.setId(id);
		post.setTitle(title);
		post.setSlug("post-" + id);
		post.setSummary("Summary");
		post.setContent(content);
		post.setContentType(PostContentType.MDX);
		post.setStatus(PostStatus.DRAFT);
		post.setIsFeatured(false);
		return post;
	}

	private User user(Long id, String username) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		return user;
	}

	private Tag tag(Long id, String slug) {
		Tag tag = new Tag();
		tag.setId(id);
		tag.setName(slug);
		tag.setSlug(slug);
		return tag;
	}

	private PostRevision latestRevision(int versionNumber, String snapshotHash) {
		PostRevision latest = new PostRevision();
		latest.setVersionNumber(versionNumber);
		latest.setSnapshotHash(snapshotHash);
		return latest;
	}
}
