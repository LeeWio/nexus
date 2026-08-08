package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostReportStatus;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.PostReportRequest;
import space.nebula.nexus.payload.request.PostReportResolutionRequest;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostReportServiceImplTest {
	@Mock
	private JdbcTemplate jdbcTemplate;
	@Mock
	private PostRepository postRepository;
	@Mock
	private UserRepository userRepository;

	private PostReportServiceImpl postReportService;
	private User reader;
	private Post publishedPost;

	@BeforeEach
	void setUp() {
		postReportService = new PostReportServiceImpl(jdbcTemplate, postRepository, userRepository);
		reader = user(42L, "reader");
		publishedPost = new Post();
		publishedPost.setId(7L);
		publishedPost.setStatus(PostStatus.PUBLISHED);
		publishedPost.setAuthor(user(9L, "author"));
	}

	@Test
	void reportPublishedPostCreatesOneOpenReport() {
		when(postRepository.findById(7L)).thenReturn(Optional.of(publishedPost));
		when(jdbcTemplate.update(contains("INSERT IGNORE INTO blog_post_report"), eq(7L), eq(42L), eq("spam"),
				isNull(), eq(PostReportStatus.OPEN.name()))).thenReturn(1);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(reader);

			var response = postReportService.reportPost(7L, new PostReportRequest(" spam ", "  "));

			assertEquals("Post report received.", response.message());
		}
	}

	@Test
	void reportPostRejectsItsAuthor() {
		publishedPost.setAuthor(reader);
		when(postRepository.findById(7L)).thenReturn(Optional.of(publishedPost));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(reader);

			BusinessException exception = assertThrows(BusinessException.class,
					() -> postReportService.reportPost(7L, new PostReportRequest("spam", null)));

			assertEquals(400, exception.getCode());
			verifyNoInteractions(jdbcTemplate);
		}
	}

	@Test
	void duplicatePostReportRemainsSuccessfulWithoutCreatingAnotherRecord() {
		when(postRepository.findById(7L)).thenReturn(Optional.of(publishedPost));
		when(jdbcTemplate.update(contains("INSERT IGNORE INTO blog_post_report"), eq(7L), eq(42L), eq("spam"),
				isNull(), eq(PostReportStatus.OPEN.name()))).thenReturn(0);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(reader);

			var response = postReportService.reportPost(7L, new PostReportRequest("spam", null));

			assertEquals("Post report was already received.", response.message());
		}
	}

	@Test
	void reportPostRejectsUnpublishedPosts() {
		publishedPost.setStatus(PostStatus.DRAFT);
		when(postRepository.findById(7L)).thenReturn(Optional.of(publishedPost));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(reader);

			BusinessException exception = assertThrows(BusinessException.class,
					() -> postReportService.reportPost(7L, new PostReportRequest("spam", null)));

			assertEquals(400, exception.getCode());
		}
	}

	@Test
	void resolveOpenReportRecordsModeratorAndResolution() {
		User moderator = user(2L, "moderator");
		when(jdbcTemplate.update(contains("UPDATE blog_post_report"), eq(PostReportStatus.DISMISSED.name()),
				eq("No policy violation."), eq("moderator"), eq(7L), eq(42L), eq(PostReportStatus.OPEN.name())))
				.thenReturn(1);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(moderator);

			var response = postReportService.resolveReport(7L, 42L,
					new PostReportResolutionRequest(PostReportStatus.DISMISSED, "No policy violation."));

			assertEquals("Post report resolved.", response.message());
		}
	}

	@Test
	void resolveReportRejectsOpenAsAFinalStatus() {
		BusinessException exception = assertThrows(BusinessException.class,
				() -> postReportService.resolveReport(7L, 42L,
						new PostReportResolutionRequest(PostReportStatus.OPEN, null)));

		assertEquals(400, exception.getCode());
		verifyNoInteractions(jdbcTemplate);
	}

	private static User user(Long id, String username) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		return user;
	}
}
