package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostCollection;
import space.nebula.nexus.entity.PostFavorite;
import space.nebula.nexus.entity.ReadingHistory;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.mapper.CategoryMapper;
import space.nebula.nexus.payload.request.PostCollectionRequest;
import space.nebula.nexus.payload.request.ReadingProgressRequest;
import space.nebula.nexus.payload.response.PostDigestResponse;
import space.nebula.nexus.repository.PostCollectionItemRepository;
import space.nebula.nexus.repository.CategoryFollowRepository;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.HiddenRecommendationRepository;
import space.nebula.nexus.repository.PostCollectionRepository;
import space.nebula.nexus.repository.PostFavoriteRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.ReadingHistoryRepository;
import space.nebula.nexus.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalLibraryServiceImplTest
{
	@Mock
	private UserRepository userRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private PostRepository postRepository;
	@Mock
	private CategoryFollowRepository categoryFollowRepository;
	@Mock
	private HiddenRecommendationRepository hiddenRecommendationRepository;
	@Mock
	private PostFavoriteRepository favoriteRepository;
	@Mock
	private ReadingHistoryRepository readingHistoryRepository;
	@Mock
	private PostCollectionRepository collectionRepository;
	@Mock
	private PostCollectionItemRepository collectionItemRepository;
	@Mock
	private PostMapper postMapper;
	@Mock
	private CategoryMapper categoryMapper;

	@InjectMocks
	private PersonalLibraryServiceImpl personalLibraryService;

	private User currentUser;

	@BeforeEach
	void setUp()
	{
		currentUser = new User();
		currentUser.setId(42L);
		currentUser.setUsername("reader");
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("reader", "password", List.of()));
		when(userRepository.findByUsername("reader")).thenReturn(Optional.of(currentUser));
	}

	@AfterEach
	void tearDown()
	{
		SecurityContextHolder.clearContext();
	}

	@Test
	void followedCategoryFeedUsesPublishedPostsOnly()
	{
		var pageable = PageRequest.of(0, 20);
		Post post = publishedPost(9L, category(3L, "Architecture"));
		when(postRepository.findFollowedCategoryFeed(42L, PostStatus.PUBLISHED, pageable))
				.thenReturn(new PageImpl<>(List.of(post), pageable, 1));
		when(postMapper.toDigestResponse(post)).thenReturn(digest(9L));

		var response = personalLibraryService.getFollowingFeed(pageable);

		assertEquals(1, response.data().getTotal());
		assertEquals(9L, response.data().getList().getFirst().id());
	}

	@Test
	void overviewCombinesDistinctPersonalSectionsAndExplainableRecommendations()
	{
		var candidatePage = PageRequest.of(0, 18);
		var preferencePage = PageRequest.of(0, 5);
		Category architecture = category(3L, "Architecture");
		Post continuingPost = publishedPost(1L, architecture);
		Post favoritePost = publishedPost(2L, architecture);
		Post personalizedPost = publishedPost(3L, architecture);
		Post fallbackPost = publishedPost(4L);

		ReadingHistory history = new ReadingHistory();
		history.setPost(continuingPost);
		history.recordProgress(45, "domain-model");
		PostFavorite duplicateFavorite = favorite(continuingPost);
		PostFavorite distinctFavorite = favorite(favoritePost);

		when(readingHistoryRepository.findContinuableHistory(42L, PostStatus.PUBLISHED, candidatePage))
				.thenReturn(List.of(history));
		when(favoriteRepository.findVisibleFavorites(42L, PostStatus.PUBLISHED, candidatePage))
				.thenReturn(new PageImpl<>(List.of(duplicateFavorite, distinctFavorite), candidatePage, 2));
		when(favoriteRepository.findPreferredCategoryIds(42L, PostStatus.PUBLISHED, preferencePage))
				.thenReturn(List.of(3L));
		when(readingHistoryRepository.findPreferredCategoryIds(42L, PostStatus.PUBLISHED, preferencePage))
				.thenReturn(List.of(3L));
		when(categoryFollowRepository.findCategoryIdsByUserId(42L)).thenReturn(List.of(3L));
		when(postRepository.findPersonalizedRecommendations(42L, List.of(3L), PostStatus.PUBLISHED, candidatePage))
				.thenReturn(List.of(personalizedPost));
		when(postRepository.findPopularUnseenPosts(42L, PostStatus.PUBLISHED, candidatePage))
				.thenReturn(List.of(personalizedPost, fallbackPost));
		when(postMapper.toDigestResponse(any(Post.class)))
				.thenAnswer(invocation -> digest(invocation.<Post>getArgument(0).getId()));

		var response = personalLibraryService.getOverview();

		assertEquals(List.of(1L), response.data().continueReading().stream().map(entry -> entry.post().id()).toList());
		assertEquals(List.of(2L), response.data().recentFavorites().stream().map(entry -> entry.post().id()).toList());
		assertEquals(List.of(3L, 4L),
				response.data().recommendations().stream().map(entry -> entry.post().id()).toList());
		assertEquals("FOLLOWED_CATEGORY", response.data().recommendations().getFirst().reasonCode());
		assertEquals("Because you follow Architecture.",
				response.data().recommendations().getFirst().reason());
		assertEquals("COMMUNITY_POPULAR", response.data().recommendations().getLast().reasonCode());
	}

	@Test
	void followingCategoryIsIdempotent()
	{
		Category architecture = category(3L, "Architecture");
		when(categoryRepository.findById(3L)).thenReturn(Optional.of(architecture));
		when(categoryFollowRepository.existsByUserIdAndCategoryIdAndIsDeletedFalse(42L, 3L))
				.thenReturn(false, true);
		when(categoryFollowRepository.countByUserIdAndIsDeletedFalse(42L)).thenReturn(2L);
		when(categoryFollowRepository.insertIgnore(42L, 3L)).thenReturn(1);

		var created = personalLibraryService.followCategory(3L);
		var duplicate = personalLibraryService.followCategory(3L);

		assertEquals("Category followed", created.message());
		assertEquals("Category is already followed", duplicate.message());
		verify(categoryFollowRepository).insertIgnore(42L, 3L);
	}

	@Test
	void hidingRecommendationIsIdempotentAndRequiresPublishedPost()
	{
		when(postRepository.findById(9L)).thenReturn(Optional.of(publishedPost(9L)));
		when(hiddenRecommendationRepository.insertIgnore(42L, 9L)).thenReturn(1, 0);

		var created = personalLibraryService.hideRecommendation(9L);
		var duplicate = personalLibraryService.hideRecommendation(9L);

		assertEquals("Recommendation hidden", created.message());
		assertEquals("Recommendation is already hidden", duplicate.message());
		verify(hiddenRecommendationRepository, org.mockito.Mockito.times(2)).insertIgnore(42L, 9L);
	}

	@Test
	void favoritesAreReturnedInRepositoryOrder()
	{
		Post post = publishedPost(9L);
		PostFavorite favorite = new PostFavorite();
		favorite.setPost(post);
		favorite.setCreatedAt(LocalDateTime.now());
		var pageable = PageRequest.of(0, 20);
		when(favoriteRepository.findVisibleFavorites(42L, PostStatus.PUBLISHED, pageable))
				.thenReturn(new PageImpl<>(List.of(favorite), pageable, 1));
		when(postMapper.toDigestResponse(post)).thenReturn(digest(9L));

		var response = personalLibraryService.getFavorites(pageable);

		assertEquals(1, response.data().getTotal());
		assertEquals(9L, response.data().getList().getFirst().post().id());
	}

	@Test
	void readingProgressCreatesAResumableCompletedEntry()
	{
		Post post = publishedPost(9L);
		when(postRepository.findById(9L)).thenReturn(Optional.of(post));
		when(readingHistoryRepository.findByUserIdAndPostIdAndIsDeletedFalse(42L, 9L))
				.thenReturn(Optional.empty());
		when(readingHistoryRepository.save(any(ReadingHistory.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(postMapper.toDigestResponse(post)).thenReturn(digest(9L));

		var response = personalLibraryService.recordReadingProgress(9L,
				new ReadingProgressRequest(100, "section-conclusion"));

		assertEquals(100, response.data().progressPercent());
		assertEquals("section-conclusion", response.data().positionAnchor());
		assertNotNull(response.data().completedAt());
		verify(readingHistoryRepository).save(any(ReadingHistory.class));
	}

	@Test
	void duplicateCollectionMembershipIsIdempotent()
	{
		PostCollection collection = ownedCollection(7L);
		when(collectionRepository.findByIdAndUserIdAndIsDeletedFalse(7L, 42L))
				.thenReturn(Optional.of(collection));
		when(postRepository.findById(9L)).thenReturn(Optional.of(publishedPost(9L)));
		when(collectionItemRepository.existsByCollectionIdAndPostIdAndIsDeletedFalse(7L, 9L))
				.thenReturn(true);

		var response = personalLibraryService.addPostToCollection(7L, 9L);

		assertTrue(response.message().contains("already"));
		verify(collectionItemRepository, never()).save(any());
	}

	@Test
	void foreignCollectionIsNotExposed()
	{
		when(collectionRepository.findByIdAndUserIdAndIsDeletedFalse(7L, 42L))
				.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> personalLibraryService.getCollectionPosts(7L, PageRequest.of(0, 20)));
		verify(collectionItemRepository, never()).findVisibleItems(any(), any(), any());
	}

	@Test
	void collectionNamesAreNormalizedBeforePersistence()
	{
		when(collectionRepository.countByUserIdAndIsDeletedFalse(42L)).thenReturn(0L);
		when(collectionRepository.existsByUserIdAndNameIgnoreCaseAndIsDeletedFalse(42L, "Backend Notes"))
				.thenReturn(false);
		when(collectionRepository.save(any(PostCollection.class))).thenAnswer(invocation ->
		{
			PostCollection collection = invocation.getArgument(0);
			collection.setId(7L);
			return collection;
		});

		var response = personalLibraryService.createCollection(
				new PostCollectionRequest("  Backend   Notes  ", "  Architecture references  "));

		assertEquals("Backend Notes", response.data().name());
		assertEquals("Architecture references", response.data().description());
	}

	private Post publishedPost(Long id)
	{
		Post post = new Post();
		post.setId(id);
		post.setStatus(PostStatus.PUBLISHED);
		return post;
	}

	private Post publishedPost(Long id, Category category)
	{
		Post post = publishedPost(id);
		post.setCategory(category);
		return post;
	}

	private Category category(Long id, String name)
	{
		Category category = new Category();
		category.setId(id);
		category.setName(name);
		return category;
	}

	private PostFavorite favorite(Post post)
	{
		PostFavorite favorite = new PostFavorite();
		favorite.setPost(post);
		favorite.setCreatedAt(LocalDateTime.now());
		return favorite;
	}

	private PostCollection ownedCollection(Long id)
	{
		PostCollection collection = new PostCollection();
		collection.setId(id);
		collection.setUser(currentUser);
		collection.setName("Architecture");
		return collection;
	}

	private PostDigestResponse digest(Long id)
	{
		return new PostDigestResponse(id, "Post", "post-" + id, null, null, "Author", null, null, 0L, 0L,
				null);
	}
}
