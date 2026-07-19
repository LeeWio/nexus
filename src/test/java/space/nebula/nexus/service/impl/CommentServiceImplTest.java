package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.event.CommentModeratedEvent;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.CommentMapper;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.SensitiveWordService;

import jakarta.servlet.http.HttpServletRequest;
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
    private CommentMapper commentMapper;
    @Mock
    private SensitiveWordService sensitiveWordService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private HttpServletRequest servletRequest;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = new User();
		testUser.setId(1L);
        testUser.setUsername("testuser");

        testPost = new Post();
        testPost.setId(1L);
        testPost.setStatus(PostStatus.PUBLISHED);
        lenient().when(commentRepository.saveAndFlush(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(100L);
            return comment;
        });
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
            verify(eventPublisher).publishEvent(any());
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
            
            assertEquals(400, response.code());
            verify(commentRepository).save(any(Comment.class));
            verify(eventPublisher).publishEvent(any());
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
    void moderateComment_RejectsNonTerminalStatus() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.moderateComment(1L, CommentStatus.PENDING));

        assertEquals(400, exception.getCode());
        verifyNoInteractions(commentRepository);
    }

	@Test
	void moderateCommentRejectsReplyWhenParentIsNotApproved()
	{
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
	void moderateCommentApprovalNotifiesAuthorAndParentAuthor()
	{
		User parentAuthor = new User();
		parentAuthor.setId(2L);
		parentAuthor.setUsername("parent-author");
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
		when(commentRepository.findById(20L)).thenReturn(Optional.of(reply));

		commentService.moderateComment(20L, CommentStatus.APPROVED);

		var eventCaptor = org.mockito.ArgumentCaptor.forClass(CommentModeratedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		CommentModeratedEvent event = eventCaptor.getValue();
		assertEquals(1L, event.getAuthorId());
		assertEquals(2L, event.getReplyRecipientId());
		assertEquals(CommentStatus.APPROVED, event.getStatus());
		assertEquals("/posts/professional-comments#comment-20", event.getLink());
	}

	@Test
	void moderateCommentRejectsParentWithApprovedReplies()
	{
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
    void searchCommentsForManagement_Success() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.Pageable.unpaged();
        Comment comment = new Comment();
        org.springframework.data.domain.Page<Comment> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(comment));
        
        when(commentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);
        when(commentMapper.toResponse(any())).thenReturn(mock(space.nebula.nexus.payload.response.CommentResponse.class));
        
        var response = commentService.searchCommentsForManagement(null, null, null, null, pageable);
        
        assertEquals(200, response.code());
        assertNotNull(response.data());
        assertEquals(1, response.data().getList().size());
    }
}
