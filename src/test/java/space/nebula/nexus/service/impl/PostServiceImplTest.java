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
import space.nebula.nexus.config.BlogDiscoveryProperties;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostSeries;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.BatchDeleteRequest;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.request.PostArchiveRequest;
import space.nebula.nexus.payload.request.PostScheduleRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.BlogDiscoveryResponse;
import space.nebula.nexus.payload.response.BlogFacetResponse;
import space.nebula.nexus.payload.response.PostDigestResponse;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IInteractionService;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.PostRankingService;
import space.nebula.nexus.utils.RedisUtil;

import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;

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
	@Spy
	private BlogDiscoveryProperties discoveryProperties = new BlogDiscoveryProperties();
	@Spy
	private PostRankingService postRankingService = new PostRankingService(discoveryProperties);
	@InjectMocks
	private PostServiceImpl postService;

	@Test
	@DisplayName("Should return paginated admin posts")
	void searchPostsForAdmin_Success() {
		// Arrange
		Pageable pageable = Pageable.unpaged();
		Post post = new Post();
		Page<Post> page = new PageImpl<>(List.of(post));
		when(postRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
				.thenReturn(page);

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
		PostRequest request = new PostRequest("My Title", null, null, "Summary", "Content", null, PostStatus.PUBLISHED,
				false, null, null, null, null, null);
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

		when(postRepository.findAllByStatus(eq(PostStatus.PUBLISHED), any(Pageable.class))).thenReturn(
				new PageImpl<>(List.of(featured, latest, anotherLatest)),
				new PageImpl<>(List.of(latest, mostRead, featured)));
		when(postRepository.findAllByStatusAndIsFeaturedTrue(eq(PostStatus.PUBLISHED), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(featured)));
		when(postMapper.toDigestResponse(any(Post.class))).thenAnswer(invocation -> {
			Post post = invocation.getArgument(0);
			return new PostDigestResponse(post.getId(), post.getTitle(), "post-" + post.getId(), null, null, null, null,
					null, post.getViews(), post.getLikesCount(), post.getPublishedAt());
		});

		ApiResponse<BlogDiscoveryResponse> response = postService.retrievePublicDiscovery();

		assertEquals(1L, response.data().spotlight().id());
		assertEquals(List.of(2L, 3L), response.data().latest().stream().map(PostDigestResponse::id).toList());
		assertEquals(List.of(4L), response.data().mostRead().stream().map(PostDigestResponse::id).toList());
	}

	@Test
	@DisplayName("Should rank curated discovery posts by editorial and engagement score")
	void retrievePublicDiscovery_ReturnsCuratedProminentPosts() {
		Post featured = post(1L, "Featured");
		featured.setIsFeatured(true);
		Post popular = post(2L, "Popular");
		popular.setViews(2_000L);
		popular.setLikesCount(30L);
		Post weak = post(3L, "Weak");
		weak.setViews(2L);

		when(postRepository.findDiscoveryCandidates(eq(PostStatus.PUBLISHED), any(Pageable.class)))
				.thenReturn(List.of(weak, popular, featured));
		when(postRepository.findAllByStatus(eq(PostStatus.PUBLISHED), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()), new PageImpl<>(List.of()));
		when(postRepository.findAllByStatusAndIsFeaturedTrue(eq(PostStatus.PUBLISHED), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(featured)));
		when(postMapper.toDigestResponse(any(Post.class))).thenAnswer(invocation -> {
			Post post = invocation.getArgument(0);
			return new PostDigestResponse(post.getId(), post.getTitle(), "post-" + post.getId(), null, null, null, null,
					null, post.getViews(), post.getLikesCount(), post.getPublishedAt());
		});

		ApiResponse<BlogDiscoveryResponse> response = postService.retrievePublicDiscovery();

		assertEquals(1L, response.data().spotlight().id());
		assertEquals(List.of(2L, 3L), response.data().curated().stream().map(PostDigestResponse::id).toList());
	}

	@Test
	@DisplayName("Should return prominent public posts page")
	void retrieveFeaturedPublicPosts_ReturnsRankedPage() {
		Post featured = post(1L, "Featured");
		Pageable pageable = Pageable.unpaged();
		when(postRepository.findProminentPublicPosts(PostStatus.PUBLISHED, pageable))
				.thenReturn(new PageImpl<>(List.of(featured)));
		when(postMapper.toDigestResponse(featured)).thenReturn(
				new PostDigestResponse(1L, "Featured", "featured", null, null, null, null, null, 0L, 0L, null));

		ApiResponse<PageResult<PostDigestResponse>> response = postService.retrieveFeaturedPublicPosts(pageable);

		assertEquals(200, response.code());
		assertEquals(List.of(1L), response.data().getList().stream().map(PostDigestResponse::id).toList());
	}

	@Test
	@DisplayName("Should return related posts ordered by shared content signals")
	void retrieveRelatedPosts_ReturnsRankedRelatedPosts() {
		Category architecture = category(7L, "Architecture");
		PostSeries series = series(9L);
		Tag java = tag(5L, "Java");
		Tag spring = tag(6L, "Spring");
		Post source = post(10L, "Source");
		source.publish();
		source.setSlug("source");
		source.setCategory(architecture);
		source.setSeries(series);
		source.setContentType(PostContentType.MDX);
		source.setTags(new java.util.HashSet<>(List.of(java, spring)));

		Post categoryOnly = post(11L, "Category only");
		categoryOnly.publish();
		categoryOnly.setCategory(architecture);
		categoryOnly.setContentType(PostContentType.MDX);
		categoryOnly.setViews(10_000L);

		Post sameSeriesAndTags = post(12L, "Same series and tags");
		sameSeriesAndTags.publish();
		sameSeriesAndTags.setSeries(series);
		sameSeriesAndTags.setCategory(architecture);
		sameSeriesAndTags.setContentType(PostContentType.MDX);
		sameSeriesAndTags.setTags(new java.util.HashSet<>(List.of(java, spring)));

		when(postRepository.findBySlug("source")).thenReturn(Optional.of(source));
		when(postRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(categoryOnly, sameSeriesAndTags)));
		when(postMapper.toDigestResponse(any(Post.class))).thenAnswer(invocation -> {
			Post post = invocation.getArgument(0);
			return new PostDigestResponse(post.getId(), post.getTitle(), "post-" + post.getId(), null, null, null, null,
					null, post.getViews(), post.getLikesCount(), post.getPublishedAt());
		});

		ApiResponse<List<PostDigestResponse>> response = postService.retrieveRelatedPosts("source",
				org.springframework.data.domain.PageRequest.of(0, 2));

		assertEquals(200, response.code());
		assertEquals(List.of(12L, 11L), response.data().stream().map(PostDigestResponse::id).toList());
	}

	@Test
	@DisplayName("Should return public archive as digest page")
	void retrievePublicArchive_ReturnsDigestPage() {
		Post archivedPost = post(13L, "Archived by month");
		archivedPost.publish();
		archivedPost.setPublishedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
		when(postRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(archivedPost)));
		when(postMapper.toDigestResponse(archivedPost)).thenReturn(new PostDigestResponse(13L, "Archived by month",
				"archived-by-month", null, null, null, null, null, 0L, 0L, archivedPost.getPublishedAt()));

		ApiResponse<PageResult<PostDigestResponse>> response = postService.retrievePublicArchive(2026, 7,
				org.springframework.data.domain.PageRequest.of(0, 10));

		assertEquals(200, response.code());
		assertEquals(List.of(13L), response.data().getList().stream().map(PostDigestResponse::id).toList());
	}

	@Test
	@DisplayName("Should reject archive month without year")
	void retrievePublicArchive_RejectsMonthWithoutYear() {
		BusinessException exception = assertThrows(BusinessException.class, () -> postService
				.retrievePublicArchive(null, 7, org.springframework.data.domain.PageRequest.of(0, 10)));

		assertEquals("Archive month requires a year", exception.getMessage());
		verify(postRepository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class),
				any(Pageable.class));
	}

	@Test
	@DisplayName("Should return public blog facets from repository aggregations")
	void retrievePublicFacets_ReturnsAggregatedCounts() {
		when(postRepository.countByStatus(PostStatus.PUBLISHED)).thenReturn(3L);
		when(postRepository.countPublishedPostsByCategory(PostStatus.PUBLISHED))
				.thenReturn(Collections.singletonList(new Object[] { 7L, "Architecture", "architecture", 2L }));
		when(postRepository.countPublishedPostsByTag(PostStatus.PUBLISHED))
				.thenReturn(Collections.singletonList(new Object[] { 5L, "Java", "java", 3L }));
		when(postRepository.countPublishedPostsByArchiveMonth(PostStatus.PUBLISHED))
				.thenReturn(Collections.singletonList(new Object[] { 2026, 7, 3L }));
		when(postRepository.countPublishedPostsByContentType(PostStatus.PUBLISHED))
				.thenReturn(Collections.singletonList(new Object[] { PostContentType.MDX, 1L }));

		ApiResponse<BlogFacetResponse> response = postService.retrievePublicFacets();

		assertEquals(200, response.code());
		assertEquals(3L, response.data().totalPublishedCount());
		assertEquals("Architecture", response.data().categories().getFirst().name());
		assertEquals("Java", response.data().tags().getFirst().name());
		assertEquals(2026, response.data().archives().getFirst().year());
		assertEquals(PostContentType.MDX, response.data().contentTypes().getFirst().contentType());
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
				() -> assertEquals(PostStatus.PUBLISHED, second.getStatus()), () -> assertNull(first.getScheduledAt()),
				() -> assertNull(second.getScheduledAt()), () -> assertNotNull(first.getPublishedAt()),
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
		PostRequest request = new PostRequest("Changed", null, null, "Summary", "Content", null, PostStatus.DRAFT,
				false, null, null, null, null, null);
		when(postRepository.findByIdForUpdate(34L)).thenReturn(Optional.of(post));

		BusinessException exception = assertThrows(BusinessException.class, () -> postService.updatePost(34L, request));

		assertEquals("Only draft or rejected posts can be edited", exception.getMessage());
		verify(postRepository, never()).save(post);
	}

	@Test
	@DisplayName("Should batch delete owned posts after validating every target")
	void deletePosts_Success() {
		User author = new User();
		author.setId(51L);
		author.setUsername("author");
		Post draft = post(41L, "Draft article");
		draft.setStatus(PostStatus.DRAFT);
		draft.setAuthor(author);
		Post rejected = post(42L, "Rejected article");
		rejected.setStatus(PostStatus.REJECTED);
		rejected.setAuthor(author);
		List<Long> postIds = List.of(41L, 42L);
		when(postRepository.findAllByIdInForUpdate(postIds)).thenReturn(List.of(draft, rejected));
		when(postRepository.findParentIdsWithChildrenOutside(postIds, postIds)).thenReturn(Set.of());

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(author);
			mockedSecurity.when(() -> SecurityUtil.hasRole("ADMIN")).thenReturn(false);
			mockedSecurity.when(() -> SecurityUtil.hasRole("EDITOR")).thenReturn(false);

			ApiResponse<Void> response = postService.deletePosts(new BatchDeleteRequest(postIds));

			assertEquals(200, response.code());
			verify(postRepository).deleteAll(List.of(draft, rejected));
			verify(eventPublisher, times(2)).publishEvent(any());
		}
	}

	@Test
	@DisplayName("Should keep a parent when a child outside the batch still exists")
	void deletePosts_RejectsParentWithUnselectedChild() {
		User author = new User();
		author.setId(52L);
		Post parent = post(43L, "Parent article");
		parent.setStatus(PostStatus.DRAFT);
		parent.setAuthor(author);
		List<Long> postIds = List.of(43L);
		when(postRepository.findAllByIdInForUpdate(postIds)).thenReturn(List.of(parent));
		when(postRepository.findParentIdsWithChildrenOutside(postIds, postIds)).thenReturn(Set.of(43L));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(author);
			mockedSecurity.when(() -> SecurityUtil.hasRole("ADMIN")).thenReturn(false);
			mockedSecurity.when(() -> SecurityUtil.hasRole("EDITOR")).thenReturn(false);

			assertThrows(BusinessException.class, () -> postService.deletePosts(new BatchDeleteRequest(postIds)));

			verify(postRepository, never()).deleteAll(any());
		}
	}

	@Test
	@DisplayName("Should copy content metadata while resetting publication state")
	void copyPost_CreatesIndependentDraft() {
		User author = new User();
		author.setId(53L);
		author.setUsername("author");
		Category category = category(6L, "Engineering");
		Tag tag = tag(7L, "Java");
		Post source = post(44L, "Source article");
		source.setSlug("source-article");
		source.setContent("# Source");
		source.setContentType(PostContentType.MDX);
		source.setSummary("Source summary");
		source.setCoverImage("https://example.com/cover.png");
		source.setCategory(category);
		source.setTags(new HashSet<>(Set.of(tag)));
		source.setStatus(PostStatus.PUBLISHED);
		source.setIsFeatured(true);
		source.setViews(120L);
		source.setLikesCount(8L);
		source.setFavoritesCount(4L);
		when(postRepository.findById(44L)).thenReturn(Optional.of(source));
		when(slugService.generateUniqueSlug(anyString(), anyString(), any())).thenReturn("source-article-copy");
		when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
			Post savedPost = invocation.getArgument(0);
			if (savedPost.getId() == null) {
				savedPost.setId(45L);
			}
			return savedPost;
		});
		PostResponse copiedResponse = mock(PostResponse.class);
		when(postMapper.toResponse(any(Post.class))).thenReturn(copiedResponse);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(author);

			ApiResponse<PostResponse> response = postService.copyPost(44L);

			ArgumentCaptor<Post> copiedPostCaptor = ArgumentCaptor.forClass(Post.class);
			verify(postRepository, times(2)).save(copiedPostCaptor.capture());
			Post copiedPost = copiedPostCaptor.getAllValues().getFirst();
			assertEquals(200, response.code());
			assertSame(copiedResponse, response.data());
			assertEquals("Source article (Copy)", copiedPost.getTitle());
			assertEquals("source-article-copy", copiedPost.getSlug());
			assertEquals(PostStatus.DRAFT, copiedPost.getStatus());
			assertFalse(copiedPost.getIsFeatured());
			assertEquals(0L, copiedPost.getViews());
			assertEquals(0L, copiedPost.getLikesCount());
			assertEquals(0L, copiedPost.getFavoritesCount());
			assertSame(author, copiedPost.getAuthor());
			assertSame(category, copiedPost.getCategory());
			assertEquals(Set.of(tag), copiedPost.getTags());
			assertNull(copiedPost.getSeries());
			assertNull(copiedPost.getParent());
			verify(eventPublisher).publishEvent(any());
		}
	}

	@Test
	@DisplayName("Should verify that public search and retrieve methods are annotated with @Transactional to prevent LazyInitializationException")
	void verifyTransactionalAnnotations() throws NoSuchMethodException {
		java.lang.reflect.Method searchMethod = PostServiceImpl.class.getMethod("searchPublicPosts", Long.class,
				Long.class, String.class, Pageable.class);
		org.springframework.transaction.annotation.Transactional searchAnnotation = searchMethod
				.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
		assertNotNull(searchAnnotation, "searchPublicPosts must be annotated with @Transactional");
		assertTrue(searchAnnotation.readOnly(), "searchPublicPosts @Transactional must be readOnly = true");

		java.lang.reflect.Method retrieveMethod = PostServiceImpl.class.getMethod("retrievePostBySlug", String.class);
		org.springframework.transaction.annotation.Transactional retrieveAnnotation = retrieveMethod
				.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
		assertNotNull(retrieveAnnotation, "retrievePostBySlug must be annotated with @Transactional");
		assertTrue(retrieveAnnotation.readOnly(), "retrievePostBySlug @Transactional must be readOnly = true");
	}

	private Post post(Long id, String title) {
		Post post = new Post();
		post.setId(id);
		post.setTitle(title);
		return post;
	}

	private Category category(Long id, String name) {
		Category category = new Category();
		category.setId(id);
		category.setName(name);
		category.setSlug(name.toLowerCase());
		return category;
	}

	private Tag tag(Long id, String name) {
		Tag tag = new Tag();
		tag.setId(id);
		tag.setName(name);
		tag.setSlug(name.toLowerCase());
		return tag;
	}

	private PostSeries series(Long id) {
		PostSeries series = new PostSeries();
		series.setId(id);
		series.setName("Series " + id);
		series.setSlug("series-" + id);
		return series;
	}
}
