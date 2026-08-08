package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import space.nebula.nexus.common.event.CommentModeratedEvent;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.CommentModerationProperties;
import space.nebula.nexus.config.CommentThreadProperties;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentReportStatus;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.request.CommentReportRequest;
import space.nebula.nexus.payload.request.CommentUpdateRequest;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.SensitiveWordService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

	@Mock
	private CommentRepository commentRepository;
	@Mock
	private PostRepository postRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private CommentResponseAssembler commentResponseAssembler;
	@Mock
	private SensitiveWordService sensitiveWordService;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	@Mock
	private HttpServletRequest servletRequest;
	@Mock
	private JdbcTemplate jdbcTemplate;
	@Mock
	private CommentGovernanceService governanceService;
	@Mock
	private CommentIdempotencyService idempotencyService;
	@Mock
	private CommentMetricsService metricsService;

	private CommentServiceImpl commentService;
	private CommentModerationProperties moderationProperties;
	private CommentThreadProperties threadProperties;

	private User testUser;
	private Post testPost;

	@BeforeEach
	void setUp() {
		testUser = new User();
		testUser.setId(1L);
		testUser.setUsername("testuser");
		testUser.setNickname("Test User");

		testPost = new Post();
		testPost.setId(1L);
		testPost.setTitle("Professional Comments");
		testPost.setStatus(PostStatus.PUBLISHED);
		moderationProperties = new CommentModerationProperties();
		threadProperties = new CommentThreadProperties();
		lenient().when(idempotencyService.hashSubmission(any(), any(), any())).thenReturn("request-hash");
		lenient().when(idempotencyService.begin(anyLong(), any(), any())).thenReturn(Optional.empty());
		lenient().when(commentRepository.saveAndFlush(any(Comment.class))).thenAnswer(invocation -> {
			Comment comment = invocation.getArgument(0);
			comment.setId(100L);
			return comment;
		});
		lenient().when(commentResponseAssembler.toResponseList(anyCollection())).thenAnswer(invocation -> {
			Collection<Comment> comments = invocation.getArgument(0);
			return comments.stream().map(comment -> CommentResponse.builder().id(comment.getId()).replyCount(0)
					.likesCount(0L).reportsCount(0L).likedByCurrentUser(false).build()).toList();
		});
		commentService = new CommentServiceImpl(
				new CommentCommandService(commentRepository, postRepository, userRepository, sensitiveWordService,
						eventPublisher, jdbcTemplate, governanceService, moderationProperties, threadProperties,
						idempotencyService, metricsService),
				new CommentQueryService(commentRepository, postRepository, userRepository, commentResponseAssembler),
				new CommentModerationService(commentRepository, eventPublisher, governanceService, metricsService),
				governanceService);
	}

	@Test
	void publishComment_Success() {
		CommentRequest request = new CommentRequest("Hello World", 1L, null);

		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(sensitiveWordService.filter("Hello World")).thenReturn("Hello World");

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			var response = commentService.publishComment(request, servletRequest);

			assertEquals(200, response.code());
			verify(commentRepository).save(any(Comment.class));
			var eventCaptor = org.mockito.ArgumentCaptor.forClass(CommentSubmittedEvent.class);
			verify(eventPublisher).publishEvent(eventCaptor.capture());
			CommentSubmittedEvent event = eventCaptor.getValue();
			assertEquals(100L, event.getCommentId());
			assertEquals("testuser", event.getAuthorUsername());
			assertEquals("Test User", event.getAuthorDisplayName());
			assertEquals(CommentStatus.PENDING, event.getStatus());
		}
	}

	@Test
	void publishComment_WithViolation() {
		CommentRequest request = new CommentRequest("Bad Word", 1L, null);

		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(sensitiveWordService.filter("Bad Word")).thenReturn("***");

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			var response = commentService.publishComment(request, servletRequest);

			assertEquals(200, response.code());
			verify(commentRepository).save(any(Comment.class));
			var eventCaptor = org.mockito.ArgumentCaptor.forClass(CommentSubmittedEvent.class);
			verify(eventPublisher).publishEvent(eventCaptor.capture());
			assertEquals(CommentStatus.SPAM, eventCaptor.getValue().getStatus());
		}
	}

	@Test
	void publishCommentWithSameIdempotencyKeyReturnsExistingSuccess() {
		CommentRequest request = new CommentRequest("Hello World", 1L, null);
		Comment existing = new Comment();
		existing.setId(101L);
		existing.setUser(testUser);
		existing.setPost(testPost);
		existing.setContent("Hello World");
		existing.setClientRequestId("comment-key-1");

		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(sensitiveWordService.filter("Hello World")).thenReturn("Hello World");
		when(servletRequest.getHeader("Idempotency-Key")).thenReturn("comment-key-1");
		when(commentRepository.findByUserIdAndClientRequestId(1L, "comment-key-1")).thenReturn(Optional.of(existing));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			var response = commentService.publishComment(request, servletRequest);

			assertEquals(200, response.code());
			verify(commentRepository, never()).saveAndFlush(any(Comment.class));
			verify(eventPublisher, never()).publishEvent(any());
		}
	}

	@Test
	void publishCommentRejectsReusedIdempotencyKeyForDifferentContent() {
		CommentRequest request = new CommentRequest("Updated content", 1L, null);
		Comment existing = new Comment();
		existing.setId(101L);
		existing.setUser(testUser);
		existing.setPost(testPost);
		existing.setContent("Original content");
		existing.setClientRequestId("comment-key-1");

		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(sensitiveWordService.filter("Updated content")).thenReturn("Updated content");
		when(servletRequest.getHeader("Idempotency-Key")).thenReturn("comment-key-1");
		when(commentRepository.findByUserIdAndClientRequestId(1L, "comment-key-1")).thenReturn(Optional.of(existing));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			BusinessException exception = assertThrows(BusinessException.class,
					() -> commentService.publishComment(request, servletRequest));

			assertEquals(40002, exception.getCode());
			verify(commentRepository, never()).saveAndFlush(any(Comment.class));
		}
	}

	@Test
	void publishCommentRejectsTooLongIdempotencyKey() {
		CommentRequest request = new CommentRequest("Hello World", 1L, null);

		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(sensitiveWordService.filter("Hello World")).thenReturn("Hello World");
		when(servletRequest.getHeader("Idempotency-Key")).thenReturn("x".repeat(81));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			BusinessException exception = assertThrows(BusinessException.class,
					() -> commentService.publishComment(request, servletRequest));

			assertEquals(400, exception.getCode());
			verify(commentRepository, never()).saveAndFlush(any(Comment.class));
		}
	}

	@Test
	void retrieveCommentsByPostRejectsUnpublishedPost() {
		testPost.setStatus(PostStatus.DRAFT);
		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> commentService.retrieveCommentsByPost(1L));

		assertEquals(403, exception.getCode());
		verify(commentRepository, never()).findAllByPostIdAndStatusOrderByPathAsc(anyLong(), any());
	}

	@Test
	void retrieveRootCommentsByPostUsesPaginatedRootQuery() {
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
		Comment comment = new Comment();
		org.springframework.data.domain.Page<Comment> page = new org.springframework.data.domain.PageImpl<>(
				java.util.List.of(comment), pageable, 1);

		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(commentRepository.findAllByPostIdAndParentIsNullAndStatus(1L, CommentStatus.APPROVED, pageable))
				.thenReturn(page);

		var response = commentService.retrieveRootCommentsByPost(1L, pageable);

		assertEquals(200, response.code());
		assertEquals(1, response.data().getTotal());
		assertEquals(0, response.data().getList().getFirst().replyCount());
		verify(commentRepository, never()).findAllByPostIdAndStatusOrderByPathAsc(anyLong(), any());
	}

	@Test
	void retrieveRootCommentsByPostRejectsUnpublishedPost() {
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
		testPost.setStatus(PostStatus.ARCHIVED);
		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> commentService.retrieveRootCommentsByPost(1L, pageable));

		assertEquals(403, exception.getCode());
		verify(commentRepository, never()).findAllByPostIdAndParentIsNullAndStatus(anyLong(), any(), any());
	}

	@Test
	void retrieveRepliesUsesPaginatedReplyQuery() {
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
		Comment parent = new Comment();
		parent.setId(10L);
		parent.setPost(testPost);
		parent.setStatus(CommentStatus.APPROVED);
		Comment reply = new Comment();
		org.springframework.data.domain.Page<Comment> page = new org.springframework.data.domain.PageImpl<>(
				java.util.List.of(reply), pageable, 1);

		when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));
		when(commentRepository.findAllByParentIdAndStatus(10L, CommentStatus.APPROVED, pageable)).thenReturn(page);

		var response = commentService.retrieveReplies(10L, pageable);

		assertEquals(200, response.code());
		assertEquals(1, response.data().getTotal());
		assertEquals(0, response.data().getList().getFirst().replyCount());
	}

	@Test
	void retrieveRepliesRejectsHiddenParent() {
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
		Comment parent = new Comment();
		parent.setId(10L);
		parent.setStatus(CommentStatus.PENDING);
		when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> commentService.retrieveReplies(10L, pageable));

		assertEquals(403, exception.getCode());
		verify(commentRepository, never()).findAllByParentIdAndStatus(anyLong(), any(), any());
	}

	@Test
	void retrieveGuestbookRootCommentsUsesPaginatedRootQuery() {
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
		Comment comment = new Comment();
		org.springframework.data.domain.Page<Comment> page = new org.springframework.data.domain.PageImpl<>(
				java.util.List.of(comment), pageable, 1);

		when(commentRepository.findAllByPostIsNullAndParentIsNullAndStatus(CommentStatus.APPROVED, pageable))
				.thenReturn(page);

		var response = commentService.retrieveGuestbookRootComments(pageable);

		assertEquals(200, response.code());
		assertEquals(1, response.data().getTotal());
		assertEquals(0, response.data().getList().getFirst().replyCount());
		verify(commentRepository, never()).findAllByPostIsNullAndStatusOrderByPathAsc(any());
	}

	@Test
	void retrieveMyCommentsUsesCurrentUserAndOptionalStatus() {
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
		Comment comment = new Comment();
		org.springframework.data.domain.Page<Comment> page = new org.springframework.data.domain.PageImpl<>(
				List.of(comment), pageable, 1);

		when(commentRepository.findAllByUserIdAndStatus(1L, CommentStatus.PENDING, pageable)).thenReturn(page);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			var response = commentService.retrieveMyComments(CommentStatus.PENDING, pageable);

			assertEquals(200, response.code());
			assertEquals(1, response.data().getTotal());
			assertEquals(0, response.data().getList().getFirst().replyCount());
		}
	}

	@Test
	void withdrawMyCommentDeletesOwnPendingComment() {
		Comment comment = new Comment();
		comment.setId(40L);
		comment.setUser(testUser);
		comment.setStatus(CommentStatus.PENDING);
		when(commentRepository.findById(40L)).thenReturn(Optional.of(comment));
		when(commentRepository.existsByParentId(40L)).thenReturn(false);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			var response = commentService.withdrawMyComment(40L);

			assertEquals(200, response.code());
			verify(commentRepository).delete(comment);
		}
	}

	@Test
	void deleteMyCommentWithRepliesCreatesPlaceholder() {
		Comment comment = new Comment();
		comment.setId(40L);
		comment.setUser(testUser);
		comment.setContent("original");
		comment.setStatus(CommentStatus.APPROVED);
		when(commentRepository.findById(40L)).thenReturn(Optional.of(comment));
		when(commentRepository.existsByParentId(40L)).thenReturn(true);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			var response = commentService.deleteMyComment(40L);

			assertEquals(200, response.code());
			assertEquals("[deleted]", comment.getContent());
			assertTrue(comment.getDeletedPlaceholder());
			verify(commentRepository).save(comment);
			verify(commentRepository, never()).delete(any(Comment.class));
		}
	}

	@Test
	void withdrawMyCommentRejectsOtherUserComment() {
		User otherUser = new User();
		otherUser.setId(2L);
		Comment comment = new Comment();
		comment.setId(40L);
		comment.setUser(otherUser);
		comment.setStatus(CommentStatus.PENDING);
		when(commentRepository.findById(40L)).thenReturn(Optional.of(comment));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			BusinessException exception = assertThrows(BusinessException.class,
					() -> commentService.withdrawMyComment(40L));

			assertEquals(403, exception.getCode());
			verify(commentRepository, never()).delete(any(Comment.class));
		}
	}

	@Test
	void updateMyCommentEditsContentAndReturnsToPendingModeration() {
		Comment comment = new Comment();
		comment.setId(41L);
		comment.setUser(testUser);
		comment.setContent("old");
		comment.setStatus(CommentStatus.APPROVED);
		when(commentRepository.findById(41L)).thenReturn(Optional.of(comment));
		when(sensitiveWordService.filter("new content")).thenReturn("new content");

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			var response = commentService.updateMyComment(41L, new CommentUpdateRequest("new content"));

			assertEquals(200, response.code());
			assertEquals("new content", comment.getContent());
			assertEquals(CommentStatus.PENDING, comment.getStatus());
			assertNotNull(comment.getEditedAt());
			verify(commentRepository).save(comment);
		}
	}

	@Test
	void updateMyCommentRejectsDeletedPlaceholder() {
		Comment comment = new Comment();
		comment.setId(41L);
		comment.setUser(testUser);
		comment.setStatus(CommentStatus.APPROVED);
		comment.setDeletedPlaceholder(true);
		when(commentRepository.findById(41L)).thenReturn(Optional.of(comment));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			BusinessException exception = assertThrows(BusinessException.class,
					() -> commentService.updateMyComment(41L, new CommentUpdateRequest("new content")));

			assertEquals(400, exception.getCode());
			verify(commentRepository, never()).save(comment);
		}
	}

	@Test
	void deleteMyCommentDeletesOwnApprovedCommentWhenNoRepliesExist() {
		Comment comment = new Comment();
		comment.setId(42L);
		comment.setUser(testUser);
		comment.setStatus(CommentStatus.APPROVED);
		when(commentRepository.findById(42L)).thenReturn(Optional.of(comment));
		when(commentRepository.existsByParentId(42L)).thenReturn(false);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(testUser);

			var response = commentService.deleteMyComment(42L);

			assertEquals(200, response.code());
			verify(commentRepository).delete(comment);
		}
	}

	@Test
	void reportCommentRecordsUniqueReport() {
		User reporter = new User();
		reporter.setId(2L);
		reporter.setUsername("reporter");
		Comment comment = new Comment();
		comment.setId(43L);
		comment.setUser(testUser);
		comment.setStatus(CommentStatus.APPROVED);
		when(commentRepository.findById(43L)).thenReturn(Optional.of(comment));
		when(jdbcTemplate.update(any(String.class), eq(43L), eq(2L), eq("spam"), eq("details"), eq("OPEN")))
				.thenReturn(1);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(reporter);

			var response = commentService.reportComment(43L, new CommentReportRequest("spam", "details"));

			assertEquals(200, response.code());
			verify(commentRepository).incrementReports(43L, 1L);
		}
	}

	@Test
	void duplicateReportOnlyIncrementsCounterOnce() {
		User reporter = new User();
		reporter.setId(2L);
		reporter.setUsername("reporter");
		Comment comment = new Comment();
		comment.setId(43L);
		comment.setUser(testUser);
		comment.setStatus(CommentStatus.APPROVED);
		when(commentRepository.findById(43L)).thenReturn(Optional.of(comment));
		when(jdbcTemplate.update(any(String.class), eq(43L), eq(2L), eq("spam"), eq("details"), eq("OPEN")))
				.thenReturn(1, 0);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(reporter);

			commentService.reportComment(43L, new CommentReportRequest("spam", "details"));
			commentService.reportComment(43L, new CommentReportRequest("spam", "details"));

			verify(commentRepository).incrementReports(43L, 1L);
			verify(metricsService).incrementReport(true);
			verify(metricsService).incrementReport(false);
		}
	}

	@Test
	void reportCommentAutoFlagsApprovedCommentWhenReportThresholdIsReached() {
		User reporter = new User();
		reporter.setId(2L);
		reporter.setUsername("reporter");
		Comment comment = new Comment();
		comment.setId(44L);
		comment.setUser(testUser);
		comment.setStatus(CommentStatus.APPROVED);
		when(commentRepository.findById(44L)).thenReturn(Optional.of(comment));
		when(jdbcTemplate.update(any(String.class), eq(44L), eq(2L), eq("abuse"), eq("details"), eq("OPEN")))
				.thenReturn(1);
		when(governanceService.countOpenReports(44L)).thenReturn(3L);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(reporter);

			var response = commentService.reportComment(44L, new CommentReportRequest("abuse", "details"));

			assertEquals(200, response.code());
			assertEquals(CommentStatus.PENDING, comment.getStatus());
			verify(commentRepository).save(comment);
			verify(governanceService).recordModeration(comment, CommentStatus.APPROVED, CommentStatus.PENDING,
					CommentModerationAction.AUTO_FLAGGED, "REPORT_THRESHOLD",
					"Comment moved back to moderation after repeated reports.", null);
		}
	}

	@Test
	void reportCommentUsesConfiguredAutoReviewThreshold() {
		moderationProperties.setAutoReviewReportThreshold(5L);
		User reporter = new User();
		reporter.setId(2L);
		reporter.setUsername("reporter");
		Comment comment = new Comment();
		comment.setId(45L);
		comment.setUser(testUser);
		comment.setStatus(CommentStatus.APPROVED);
		when(commentRepository.findById(45L)).thenReturn(Optional.of(comment));
		when(jdbcTemplate.update(any(String.class), eq(45L), eq(2L), eq("abuse"), eq("details"), eq("OPEN")))
				.thenReturn(1);
		when(governanceService.countOpenReports(45L)).thenReturn(4L);

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(reporter);

			var response = commentService.reportComment(45L, new CommentReportRequest("abuse", "details"));

			assertEquals(200, response.code());
			assertEquals(CommentStatus.APPROVED, comment.getStatus());
			verify(commentRepository, never()).save(comment);
			verify(governanceService, never()).recordModeration(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void publishComment_RejectsReplyToUnapprovedParent() {
		Comment parent = new Comment();
		parent.setId(10L);
		parent.setPost(testPost);
		parent.setStatus(CommentStatus.PENDING);
		CommentRequest request = new CommentRequest("Reply", 1L, 10L);

		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(sensitiveWordService.filter("Reply")).thenReturn("Reply");
		when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> commentService.publishComment(request, servletRequest));

		assertEquals(400, exception.getCode());
		verify(commentRepository, never()).saveAndFlush(any());
	}

	@Test
	void publishCommentRejectsReplyToDeletedPlaceholder() {
		Comment parent = new Comment();
		parent.setId(10L);
		parent.setPost(testPost);
		parent.setStatus(CommentStatus.APPROVED);
		parent.setDeletedPlaceholder(true);
		CommentRequest request = new CommentRequest("Reply", 1L, 10L);

		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(sensitiveWordService.filter("Reply")).thenReturn("Reply");
		when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> commentService.publishComment(request, servletRequest));

		assertEquals(400, exception.getCode());
		verify(commentRepository, never()).saveAndFlush(any());
	}

	@Test
	void publishCommentRejectsReplyBeyondConfiguredThreadDepth() {
		threadProperties.setMaxReplyDepth(2);
		Comment parent = new Comment();
		parent.setId(10L);
		parent.setPost(testPost);
		parent.setStatus(CommentStatus.APPROVED);
		parent.setPath("/1/2/10/");
		CommentRequest request = new CommentRequest("Reply", 1L, 10L);

		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(sensitiveWordService.filter("Reply")).thenReturn("Reply");
		when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> commentService.publishComment(request, servletRequest));

		assertEquals(400, exception.getCode());
		verify(commentRepository, never()).saveAndFlush(any());
	}

	@Test
	void moderateComment_RejectsNonTerminalStatus() {
		BusinessException exception = assertThrows(BusinessException.class,
				() -> commentService.moderateComment(1L, CommentStatus.PENDING));

		assertEquals(400, exception.getCode());
		verifyNoInteractions(commentRepository);
	}

	@Test
	void moderateCommentRejectsReplyWhenParentIsNotApproved() {
		Comment parent = new Comment();
		parent.setStatus(CommentStatus.REJECTED);
		Comment reply = new Comment();
		reply.setParent(parent);
		reply.setStatus(CommentStatus.PENDING);
		when(commentRepository.findById(20L)).thenReturn(Optional.of(reply));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> commentService.moderateComment(20L, CommentStatus.APPROVED));

		assertEquals(400, exception.getCode());
		verify(commentRepository, never()).save(reply);
	}

	@Test
	void moderateCommentApprovalNotifiesAuthorAndParentAuthor() {
		User parentAuthor = new User();
		parentAuthor.setId(2L);
		parentAuthor.setUsername("parent-author");
		User postAuthor = new User();
		postAuthor.setId(3L);
		postAuthor.setUsername("post-author");
		Comment parent = new Comment();
		parent.setStatus(CommentStatus.APPROVED);
		parent.setUser(parentAuthor);

		Comment reply = new Comment();
		reply.setId(20L);
		reply.setParent(parent);
		reply.setPost(testPost);
		reply.setUser(testUser);
		reply.setStatus(CommentStatus.PENDING);
		testPost.setSlug("professional-comments");
		testPost.setAuthor(postAuthor);
		when(commentRepository.findById(20L)).thenReturn(Optional.of(reply));

		commentService.moderateComment(20L, CommentStatus.APPROVED);

		var eventCaptor = org.mockito.ArgumentCaptor.forClass(CommentModeratedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		CommentModeratedEvent event = eventCaptor.getValue();
		assertEquals(1L, event.getAuthorId());
		assertEquals(2L, event.getReplyRecipientId());
		assertEquals(3L, event.getPostAuthorId());
		assertEquals("Professional Comments", event.getPostTitle());
		assertEquals(CommentStatus.APPROVED, event.getStatus());
		assertEquals("/posts/professional-comments#comment-20", event.getLink());
		verify(governanceService).recordModeration(reply, CommentStatus.PENDING, CommentStatus.APPROVED,
				CommentModerationAction.STATUS_CHANGED, "MANUAL_MODERATION", null, null);
		verify(governanceService).resolveOpenReports(20L, CommentReportStatus.DISMISSED,
				"Comment approved by moderator.");
	}

	@Test
	void moderateCommentRejectsParentWithApprovedReplies() {
		Comment parent = new Comment();
		parent.setId(10L);
		parent.setStatus(CommentStatus.APPROVED);
		when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));
		when(commentRepository.existsByParentIdAndStatus(10L, CommentStatus.APPROVED)).thenReturn(true);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> commentService.moderateComment(10L, CommentStatus.REJECTED));

		assertEquals(400, exception.getCode());
		verify(commentRepository, never()).save(parent);
	}

	@Test
	void moderateCommentCanMarkCommentAsSpam() {
		Comment comment = new Comment();
		comment.setId(30L);
		comment.setUser(testUser);
		comment.setStatus(CommentStatus.PENDING);
		when(commentRepository.findById(30L)).thenReturn(Optional.of(comment));

		commentService.moderateComment(30L, CommentStatus.SPAM);

		assertEquals(CommentStatus.SPAM, comment.getStatus());
		verify(commentRepository).save(comment);
		var eventCaptor = org.mockito.ArgumentCaptor.forClass(CommentModeratedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		assertEquals(CommentStatus.SPAM, eventCaptor.getValue().getStatus());
	}

	@Test
	void batchModerateCommentsDeduplicatesIdsAndCountsChanges() {
		Comment first = new Comment();
		first.setId(51L);
		first.setUser(testUser);
		first.setStatus(CommentStatus.PENDING);
		Comment second = new Comment();
		second.setId(52L);
		second.setUser(testUser);
		second.setStatus(CommentStatus.APPROVED);

		when(commentRepository.findById(51L)).thenReturn(Optional.of(first));
		when(commentRepository.findById(52L)).thenReturn(Optional.of(second));

		var response = commentService.batchModerateComments(List.of(51L, 51L, 52L), CommentStatus.APPROVED);

		assertEquals(200, response.code());
		assertEquals(1, response.data());
		assertEquals(CommentStatus.APPROVED, first.getStatus());
		verify(commentRepository).save(first);
		verify(commentRepository, never()).save(second);
		verify(eventPublisher).publishEvent(any(CommentModeratedEvent.class));
		verify(governanceService).recordModeration(eq(first), eq(CommentStatus.PENDING), eq(CommentStatus.APPROVED),
				eq(CommentModerationAction.STATUS_CHANGED), eq("BATCH_MODERATION"), isNull(), any(String.class));
	}

	@Test
	void retrieveRootCommentsByPostCursorReturnsNextCursorWhenMoreRowsExist() {
		Comment first = commentWithId(100L);
		Comment second = commentWithId(90L);
		Comment overflow = commentWithId(80L);

		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(commentRepository.findAllByPostIdAndParentIsNullAndStatusOrderByIdDesc(eq(1L), eq(CommentStatus.APPROVED),
				any(org.springframework.data.domain.Pageable.class))).thenReturn(List.of(first, second, overflow));

		var response = commentService.retrieveRootCommentsByPostCursor(1L, null, 2);

		assertEquals(200, response.code());
		assertTrue(response.data().hasMore());
		assertEquals(90L, response.data().nextCursor());
		assertEquals(2, response.data().list().size());
	}

	@Test
	void retrieveRootCommentsByPostCursorUsesCursorWhenProvided() {
		Comment comment = commentWithId(70L);
		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(commentRepository.findAllByPostIdAndParentIsNullAndStatusAndIdLessThanOrderByIdDesc(eq(1L),
				eq(CommentStatus.APPROVED), eq(80L), any(org.springframework.data.domain.Pageable.class)))
				.thenReturn(List.of(comment));

		var response = commentService.retrieveRootCommentsByPostCursor(1L, 80L, 20);

		assertEquals(200, response.code());
		assertFalse(response.data().hasMore());
		assertNull(response.data().nextCursor());
		verify(commentRepository, never()).findAllByPostIdAndParentIsNullAndStatusOrderByIdDesc(anyLong(), any(),
				any());
	}

	@Test
	void retrieveRepliesCursorUsesAscendingCursor() {
		Comment parent = new Comment();
		parent.setId(10L);
		parent.setPost(testPost);
		parent.setStatus(CommentStatus.APPROVED);
		Comment reply = commentWithId(11L);

		when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));
		when(commentRepository.findAllByParentIdAndStatusAndIdGreaterThanOrderByIdAsc(eq(10L),
				eq(CommentStatus.APPROVED), eq(9L), any(org.springframework.data.domain.Pageable.class)))
				.thenReturn(List.of(reply));

		var response = commentService.retrieveRepliesCursor(10L, 9L, 20);

		assertEquals(200, response.code());
		assertEquals(1, response.data().list().size());
		verify(commentRepository).findAllByParentIdAndStatusAndIdGreaterThanOrderByIdAsc(eq(10L),
				eq(CommentStatus.APPROVED), eq(9L), any(org.springframework.data.domain.Pageable.class));
	}

	@Test
	void retrieveGuestbookRootCommentsCursorUsesGuestbookQuery() {
		Comment first = commentWithId(12L);
		when(commentRepository.findAllByPostIsNullAndParentIsNullAndStatusOrderByIdDesc(eq(CommentStatus.APPROVED),
				any(org.springframework.data.domain.Pageable.class))).thenReturn(List.of(first));

		var response = commentService.retrieveGuestbookRootCommentsCursor(null, 20);

		assertEquals(200, response.code());
		assertFalse(response.data().hasMore());
		assertEquals(1, response.data().list().size());
	}

	@Test
	void countNewRootCommentsByPostValidatesPublishedPost()
	{
		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
		when(commentRepository.countByPostIdAndParentIsNullAndStatusAndIdGreaterThan(1L, CommentStatus.APPROVED, 55L))
				.thenReturn(3L);

		var response = commentService.countNewRootCommentsByPost(1L, 55L);

		assertEquals(200, response.code());
		assertEquals(3L, response.data());
	}

	@Test
	void countNewRootCommentsByPostRejectsUnpublishedPost() {
		testPost.setStatus(PostStatus.DRAFT);
		when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> commentService.countNewRootCommentsByPost(1L, 55L));

		assertEquals(403, exception.getCode());
		verify(commentRepository, never()).countByPostIdAndParentIsNullAndStatusAndIdGreaterThan(anyLong(), any(),
				anyLong());
	}

	@Test
	void countNewGuestbookRootCommentsUsesZeroWhenAfterIdMissing()
	{
		when(commentRepository.countByPostIsNullAndParentIsNullAndStatusAndIdGreaterThan(CommentStatus.APPROVED, 0L))
				.thenReturn(5L);

		var response = commentService.countNewGuestbookRootComments(null);

		assertEquals(200, response.code());
		assertEquals(5L, response.data());
	}

	@Test
	void searchCommentsForManagement_Success() {
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.Pageable.unpaged();
		Comment comment = new Comment();
		org.springframework.data.domain.Page<Comment> page = new org.springframework.data.domain.PageImpl<>(
				java.util.List.of(comment));

		when(commentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
				.thenReturn(page);

		var response = commentService.searchCommentsForManagement(null, null, null, null, pageable);

		assertEquals(200, response.code());
		assertNotNull(response.data());
		assertEquals(1, response.data().getList().size());
	}

	private Comment commentWithId(Long id) {
		Comment comment = new Comment();
		comment.setId(id);
		return comment;
	}
}
