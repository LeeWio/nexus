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
        testUser.setUsername("testuser");

        testPost = new Post();
        testPost.setId(1L);
        testPost.setStatus(PostStatus.PUBLISHED);
    }

    @Test
    void publishComment_Success() {
        CommentRequest request = new CommentRequest("Hello World", 1L, null);
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(sensitiveWordService.containsSensitiveWord("Hello World")).thenReturn(false);
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
        when(sensitiveWordService.containsSensitiveWord("Bad Word")).thenReturn(true);
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
