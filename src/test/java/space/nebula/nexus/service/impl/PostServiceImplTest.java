package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.request.PostArchiveRequest;
import space.nebula.nexus.payload.request.PostScheduleRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.BlogDiscoveryResponse;
import space.nebula.nexus.payload.response.PostDigestResponse;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IInteractionService;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.utils.RedisUtil;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

	@Mock
	private PostRepository postRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private TagRepository tagRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private PostMapper postMapper;
	@Mock
	private RedisUtil redisUtil;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	@Mock
	private IInteractionService interactionService;

	@Mock
	private space.nebula.nexus.repository.PostSeriesRepository seriesRepository;
	@Mock
	private space.nebula.nexus.service.ISlugService slugService;
	@Mock
	private space.nebula.nexus.common.validator.PostValidator postValidator;
	@Mock
	private space.nebula.nexus.repository.ConfigRepository configRepository;
	@InjectMocks
	private PostServiceImpl postService;

	@Test
	@DisplayName("Should return paginated admin posts")
	void searchPostsForAdmin_Success() {
		// Arrange
		Pageable pageable = Pageable.unpaged();
		Post post = new Post();
		Page<Post> page = new PageImpl<>(List.of(post));
		when(postRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);

		PostResponse response = mock(PostResponse.class);
		when(postMapper.toResponse(any())).thenReturn(response);

		// Act
		ApiResponse<PageResult<PostResponse>> apiResponse = postService.searchPostsForAdmin(null, null, null, pageable);

		// Assert
		assertEquals(200, apiResponse.code());
		assertEquals(1, apiResponse.data().getList().size());
	}

	@Test
	@DisplayName("Should create a new post and publish event")
	void createPost_Success() {
		// Arrange
		PostRequest request = new PostRequest("My Title", null, null, "Summary", "Content", null, PostStatus.PUBLISHED, false,
				null, null, null, null, null);
		User author = new User();
		author.setUsername("admin");

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(author);
			when(slugService.generateUniqueSlug(any(), any(), any())).thenReturn("my-title");

			// Act
			ApiResponse<PostResponse> response = postService.createPost(request);

			// Assert
			assertEquals(200, response.code());
			verify(postRepository, times(2)).save(any(Post.class));
			ArgumentCaptor<Post> savedPost = ArgumentCaptor.forClass(Post.class);
			verify(postRepository, times(2)).save(savedPost.capture());
			assertTrue(savedPost.getAllValues().stream().allMatch(post -> post.getStatus() == PostStatus.DRAFT));
			verify(eventPublisher).publishEvent(any());
		}
	}

	@Test
	@DisplayName("Should build distinct discovery sections with featured content first")
	void retrievePublicDiscovery_ReturnsDistinctSections() {
		Post featured = post(1L, "Featured");
		Post latest = post(2L, "Latest");
		Post anotherLatest = post(3L, "Another latest");
		Post mostRead = post(4L, "Most read");

		when(postRepository.findAllByStatus(eq(PostStatus.PUBLISHED), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(featured, latest, anotherLatest)),
						new PageImpl<>(List.of(latest, mostRead, featured)));
		when(postRepository.findAllByStatusAndIsFeaturedTrue(eq(PostStatus.PUBLISHED), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(featured)));
		when(postMapper.toDigestResponse(any(Post.class))).thenAnswer(invocation -> {
			Post post = invocation.getArgument(0);
			return new PostDigestResponse(post.getId(), post.getTitle(), "post-" + post.getId(), null, null,
					null, null, null, post.getViews(), post.getLikesCount(), post.getPublishedAt());
		});

		ApiResponse<BlogDiscoveryResponse> response = postService.retrievePublicDiscovery();

		assertEquals(1L, response.data().spotlight().id());
		assertEquals(List.of(2L, 3L), response.data().latest().stream().map(PostDigestResponse::id).toList());
		assertEquals(List.of(4L), response.data().mostRead().stream().map(PostDigestResponse::id).toList());
	}

	@Test
	@DisplayName("Should approve and schedule a post awaiting review")
	void schedulePost_Success() {
		Post post = post(11L, "Scheduled article");
		post.setStatus(PostStatus.PENDING_REVIEW);
		User reviewer = new User();
		reviewer.setUsername("editor");
		LocalDateTime scheduledAt = LocalDateTime.now().plusHours(2);
		when(postRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(post));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(reviewer);

			ApiResponse<Void> response = postService.schedulePost(11L, new PostScheduleRequest(scheduledAt));

			assertEquals(200, response.code());
			assertEquals(PostStatus.SCHEDULED, post.getStatus());
			assertEquals(scheduledAt, post.getScheduledAt());
			assertNull(post.getPublishedAt());
			assertSame(reviewer, post.getReviewedBy());
			verify(postRepository).save(post);
			verify(eventPublisher).publishEvent(any());
		}
	}

	@Test
	@DisplayName("Should cancel a scheduled publication and return it to review")
	void cancelScheduledPost_Success() {
		Post post = post(12L, "Canceled schedule");
		post.setStatus(PostStatus.SCHEDULED);
		post.setScheduledAt(LocalDateTime.now().plusDays(1));
		post.setReviewedAt(LocalDateTime.now());
		post.setReviewedBy(new User());
		when(postRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(post));

		ApiResponse<Void> response = postService.cancelScheduledPost(12L);

		assertEquals(200, response.code());
		assertEquals(PostStatus.PENDING_REVIEW, post.getStatus());
		assertNull(post.getScheduledAt());
		assertNull(post.getReviewedAt());
		assertNull(post.getReviewedBy());
		verify(postRepository).save(post);
		verify(eventPublisher).publishEvent(any());
	}

	@Test
	@DisplayName("Should publish due posts through the full event workflow")
	void publishDueScheduledPosts_Success() {
		LocalDateTime cutoff = LocalDateTime.now();
		Post first = post(21L, "First due post");
		first.setStatus(PostStatus.SCHEDULED);
		first.setScheduledAt(cutoff.minusMinutes(2));
		Post second = post(22L, "Second due post");
		second.setStatus(PostStatus.SCHEDULED);
		second.setScheduledAt(cutoff.minusMinutes(1));
		when(postRepository.findDueScheduledPostIds(eq(PostStatus.SCHEDULED), eq(cutoff), any(Pageable.class)))
				.thenReturn(List.of(21L, 22L));
		when(postRepository.findScheduledPublicationBatch(PostStatus.SCHEDULED, List.of(21L, 22L)))
				.thenReturn(List.of(first, second));

		int published = postService.publishDueScheduledPosts(cutoff, 100);

		assertEquals(2, published);
		assertAll(() -> assertEquals(PostStatus.PUBLISHED, first.getStatus()),
				() -> assertEquals(PostStatus.PUBLISHED, second.getStatus()),
				() -> assertNull(first.getScheduledAt()),
				() -> assertNull(second.getScheduledAt()),
				() -> assertNotNull(first.getPublishedAt()),
				() -> assertNotNull(second.getPublishedAt()));
		verify(postRepository).saveAll(List.of(first, second));
		verify(eventPublisher, times(2)).publishEvent(any());
	}

	@Test
	@DisplayName("Should withdraw a pending post to draft")
	void withdrawFromReview_Success() {
		Post post = post(31L, "Pending article");
		post.setStatus(PostStatus.PENDING_REVIEW);
		post.setReviewComment("Previous review");
		when(postRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(post));

		ApiResponse<Void> response = postService.withdrawFromReview(31L);

		assertEquals(200, response.code());
		assertEquals(PostStatus.DRAFT, post.getStatus());
		assertNull(post.getReviewComment());
		verify(postRepository).save(post);
		verify(eventPublisher).publishEvent(any());
	}

	@Test
	@DisplayName("Should archive a published post with editor audit data")
	void archivePost_Success() {
		Post post = post(32L, "Published article");
		post.publish();
		post.setIsFeatured(true);
		User editor = new User();
		editor.setUsername("editor");
		when(postRepository.findByIdForUpdate(32L)).thenReturn(Optional.of(post));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(editor);

			ApiResponse<Void> response = postService.archivePost(32L,
					new PostArchiveRequest("  Requires substantial revision  "));

			assertEquals(200, response.code());
			assertEquals(PostStatus.ARCHIVED, post.getStatus());
			assertEquals("Requires substantial revision", post.getArchiveReason());
			assertNotNull(post.getArchivedAt());
			assertSame(editor, post.getArchivedBy());
			assertFalse(post.getIsFeatured());
			assertNotNull(post.getPublishedAt());
			verify(postRepository).save(post);
			verify(eventPublisher).publishEvent(any());
		}
	}

	@Test
	@DisplayName("Should restore an archived post to a clean draft")
	void restoreArchivedPost_Success() {
		Post post = post(33L, "Archived article");
		post.publish();
		post.archive("Outdated", new User());
		when(postRepository.findByIdForUpdate(33L)).thenReturn(Optional.of(post));

		ApiResponse<Void> response = postService.restoreArchivedPost(33L);

		assertEquals(200, response.code());
		assertEquals(PostStatus.DRAFT, post.getStatus());
		assertNull(post.getPublishedAt());
		assertNull(post.getArchiveReason());
		assertNull(post.getArchivedAt());
		assertNull(post.getArchivedBy());
		verify(postRepository).save(post);
		verify(eventPublisher).publishEvent(any());
	}

	@Test
	@DisplayName("Should reject standard edits for published posts")
	void updatePost_PublishedPostRejected() {
		Post post = post(34L, "Published article");
		post.publish();
		PostRequest request = new PostRequest("Changed", null, null, "Summary", "Content", null,
				PostStatus.DRAFT, false, null, null, null, null, null);
		when(postRepository.findByIdForUpdate(34L)).thenReturn(Optional.of(post));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> postService.updatePost(34L, request));

		assertEquals("Only draft or rejected posts can be edited", exception.getMessage());
		verify(postRepository, never()).save(post);
	}

	@Test
	@DisplayName("Should verify that public search and retrieve methods are annotated with @Transactional to prevent LazyInitializationException")
	void verifyTransactionalAnnotations() throws NoSuchMethodException {
		java.lang.reflect.Method searchMethod = PostServiceImpl.class.getMethod("searchPublicPosts",
				Long.class, Long.class, String.class, Pageable.class);
		org.springframework.transaction.annotation.Transactional searchAnnotation =
				searchMethod.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
		assertNotNull(searchAnnotation, "searchPublicPosts must be annotated with @Transactional");
		assertTrue(searchAnnotation.readOnly(), "searchPublicPosts @Transactional must be readOnly = true");

		java.lang.reflect.Method retrieveMethod = PostServiceImpl.class.getMethod("retrievePostBySlug", String.class);
		org.springframework.transaction.annotation.Transactional retrieveAnnotation =
				retrieveMethod.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
		assertNotNull(retrieveAnnotation, "retrievePostBySlug must be annotated with @Transactional");
		assertTrue(retrieveAnnotation.readOnly(), "retrievePostBySlug @Transactional must be readOnly = true");
	}

	private Post post(Long id, String title) {
		Post post = new Post();
		post.setId(id);
		post.setTitle(title);
		return post;
	}
}
