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
import space.nebula.nexus.service.INotificationService;
import space.nebula.nexus.enums.NotificationType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostReportServiceImplTest {
	@Mock
	private JdbcTemplate jdbcTemplate;
	@Mock
	private PostRepository postRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private INotificationService notificationService;

	private PostReportServiceImpl postReportService;
	private User reader;
	private Post publishedPost;

	@BeforeEach
	void setUp() {
		postReportService = new PostReportServiceImpl(jdbcTemplate, postRepository, userRepository,
				notificationService);
		reader = user(42L, "reader");
		publishedPost = new Post();
		publishedPost.setId(7L);
		publishedPost.setStatus(PostStatus.PUBLISHED);
		publishedPost.setAuthor(user(9L, "author"));
	}

	@Test
	void reportPublishedPostCreatesOneOpenReport() {
		when(postRepository.findById(7L)).thenReturn(Optional.of(publishedPost));
		when(jdbcTemplate.update(contains("INSERT IGNORE INTO blog_post_report"), eq(7L), eq(42L), eq("spam"), isNull(),
				eq(PostReportStatus.OPEN.name()))).thenReturn(1);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(reader);

			var response = postReportService.reportPost(7L, new PostReportRequest(" spam ", "  "));

			assertEquals("Post report received.", response.message());
			verify(notificationService).sendToAdministrators("Post report received",
					"\"null\" was reported and needs review.", NotificationType.POST_REPORT_RECEIVED,
					"/posts?tab=reports&postId=7", "REPORT:7:42", 42L,
					new space.nebula.nexus.payload.response.NotificationContext(
							space.nebula.nexus.payload.response.NotificationContext.ObjectType.POST, 7L, 42L,
							space.nebula.nexus.payload.response.NotificationContext.Action.REVIEW_REPORT));
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
		when(jdbcTemplate.update(contains("INSERT IGNORE INTO blog_post_report"), eq(7L), eq(42L), eq("spam"), isNull(),
				eq(PostReportStatus.OPEN.name()))).thenReturn(0);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(reader);

			var response = postReportService.reportPost(7L, new PostReportRequest("spam", null));

			assertEquals("Post report was already received.", response.message());
			verifyNoInteractions(notificationService);
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
		when(userRepository.findById(42L)).thenReturn(Optional.of(reader));
		when(jdbcTemplate.update(contains("UPDATE blog_post_report"), eq(PostReportStatus.DISMISSED.name()),
				eq("No policy violation."), eq("moderator"), eq(7L), eq(42L), eq(PostReportStatus.OPEN.name())))
				.thenReturn(1);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(moderator);

			var response = postReportService.resolveReport(7L, 42L,
					new PostReportResolutionRequest(PostReportStatus.DISMISSED, "No policy violation."));

			assertEquals("Post report resolved.", response.message());
			verify(notificationService).sendOnce(reader, "Post report reviewed",
					"Your report has been reviewed. Result: DISMISSED.", NotificationType.POST_REPORT_RESOLVED, null,
					"REPORT:7:42",
					new space.nebula.nexus.payload.response.NotificationContext(
							space.nebula.nexus.payload.response.NotificationContext.ObjectType.POST, 7L, 2L,
							space.nebula.nexus.payload.response.NotificationContext.Action.VIEW));
		}
	}

	@Test
	void resolveReportRejectsOpenAsAFinalStatus() {
		BusinessException exception = assertThrows(BusinessException.class, () -> postReportService.resolveReport(7L,
				42L, new PostReportResolutionRequest(PostReportStatus.OPEN, null)));

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
