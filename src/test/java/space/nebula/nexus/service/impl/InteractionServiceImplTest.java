package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.utils.RedisUtil;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionServiceImplTest {

	@Mock
	private RedisUtil redisUtil;
	@Mock
	private PostRepository postRepository;
	@Mock
	private CommentRepository commentRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private JdbcTemplate jdbcTemplate;
	@Mock
	private CacheManager cacheManager;

	private InteractionServiceImpl interactionService;
	private User user;
	private Comment comment;

	@BeforeEach
	void setUp() {
		interactionService = new InteractionServiceImpl(redisUtil, postRepository, commentRepository, userRepository,
				jdbcTemplate, cacheManager);
		user = new User();
		user.setId(1L);
		user.setUsername("reader");
		comment = new Comment();
		comment.setId(10L);
		comment.setUser(user);
		comment.setStatus(CommentStatus.APPROVED);
		when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
	}

	@Test
	void duplicateCommentLikeOnlyIncrementsCounterOnce()
	{
		when(jdbcTemplate.update(anyString(), eq(10L), eq(1L))).thenReturn(1, 0);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class))
		{
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(user);

			ApiResponse<Void> first = interactionService.likeComment(10L);
			ApiResponse<Void> duplicate = interactionService.likeComment(10L);

			assertEquals(200, first.code());
			assertEquals(200, duplicate.code());
			verify(commentRepository).incrementLikes(10L, 1L);
		}
	}

	@Test
	void concurrentDuplicateLikeOnlyIncrementsCounterOnce() {
		AtomicInteger inserted = new AtomicInteger();
		when(jdbcTemplate.update(anyString(), eq(10L), eq(1L)))
				.thenAnswer(invocation -> inserted.compareAndSet(0, 1) ? 1 : 0);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(user);

			interactionService.likeComment(10L);
			interactionService.likeComment(10L);

			verify(commentRepository).incrementLikes(10L, 1L);
		}
	}

	@Test
	void duplicateCommentUnlikeOnlyDecrementsCounterOnce()
	{
		when(jdbcTemplate.update(anyString(), eq(10L), eq(1L))).thenReturn(1, 0);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class))
		{
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(user);

			interactionService.unlikeComment(10L);
			interactionService.unlikeComment(10L);

			verify(commentRepository).incrementLikes(10L, -1L);
		}
	}

	@Test
	void deletedPlaceholderCannotBeLiked() {
		comment.setDeletedPlaceholder(true);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(user);

			BusinessException exception = assertThrows(BusinessException.class,
					() -> interactionService.likeComment(10L));

			assertEquals(400, exception.getCode());
			verify(jdbcTemplate, never()).update(anyString(), any(), any());
		}
	}
}
