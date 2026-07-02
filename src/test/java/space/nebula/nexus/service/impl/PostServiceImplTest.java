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
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.response.PageResult;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
	private space.nebula.nexus.service.IPostRevisionService postRevisionService;

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
			verify(eventPublisher).publishEvent(any());
		}
	}
}
