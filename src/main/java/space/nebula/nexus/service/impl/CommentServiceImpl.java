package space.nebula.nexus.service.impl;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.CommentMapper;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.ICommentService;
import space.nebula.nexus.service.SensitiveWordService;

/**
 * Implementation of ICommentService with hierarchical support, moderation,
 * sensitive word filtering, and async event notification.
 */
@Slf4j
@Service
public class CommentServiceImpl implements ICommentService {

    @Resource
    private CommentRepository commentRepository;

    @Resource
    private PostRepository postRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private SensitiveWordService sensitiveWordService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @LogOperation("Submit Comment")
    public ApiResponse<Void> submitComment(CommentRequest request, HttpServletRequest servletRequest) {
        // 1. Validate Post
        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new BusinessException(404, "Post not found"));
        
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new BusinessException(403, "Cannot comment on unpublished posts");
        }

        // 2. Sensitive word filtering & Auto-Moderation
        boolean hasSensitiveWords = sensitiveWordService.containsSensitiveWord(request.content());
        String filteredContent = sensitiveWordService.filter(request.content());

        // 3. Validate Parent if exists
        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(404, "Parent comment not found"));
            if (!parent.getPost().getId().equals(post.getId())) {
                throw new BusinessException(400, "Parent comment belongs to a different post");
            }
        }

        // 4. Get Current User
        User user = getCurrentUserOrThrow();

        // 5. Create and Save Comment
        Comment comment = new Comment();
        comment.setContent(filteredContent);
        comment.setPost(post);
        comment.setUser(user);
        comment.setParent(parent);
        
        // Auto-reject if sensitive words detected
        if (hasSensitiveWords) {
            comment.setStatus(CommentStatus.REJECTED);
            log.warn("Comment by {} rejected due to sensitive words: {}", user.getUsername(), request.content());
        } else {
            comment.setStatus(CommentStatus.PENDING);
        }

        comment.setIpAddress(getIpAddress(servletRequest));
        comment.setUserAgent(servletRequest.getHeader("User-Agent"));

        commentRepository.save(comment);
        
        // 6. Publish async notification event
        eventPublisher.publishEvent(new CommentSubmittedEvent(this, comment));

        if (hasSensitiveWords) {
            return ApiResponse.error(400, "Comment contains prohibited content and has been rejected.");
        }
        
        return ApiResponse.success("Comment submitted successfully and is awaiting moderation", null);
    }

    @Override
    public ApiResponse<PageResult<CommentResponse>> getPostComments(Long postId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findAllByPostIdAndParentIsNullAndStatus(
                postId, CommentStatus.APPROVED, pageable);
        
        return ApiResponse.success(PageResult.of(comments.map(commentMapper::toResponse)));
    }

    @Override
    public ApiResponse<PageResult<CommentResponse>> getAdminComments(Pageable pageable) {
        Page<Comment> comments = commentRepository.findAll(pageable);
        return ApiResponse.success(PageResult.of(comments.map(commentMapper::toResponse)));
    }

    @Override
    @Transactional
    @LogOperation("Moderate Comment")
    public ApiResponse<Void> updateCommentStatus(Long id, CommentStatus status) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Comment not found"));
        
        comment.setStatus(status);
        commentRepository.save(comment);
        log.info("Comment {} status updated to {}", id, status);
        return ApiResponse.success("Comment status updated successfully", null);
    }

    @Override
    @Transactional
    @LogOperation("Delete Comment")
    public ApiResponse<Void> deleteComment(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new BusinessException(404, "Comment not found");
        }
        commentRepository.deleteById(id);
        log.info("Comment {} deleted", id);
        return ApiResponse.success("Comment deleted successfully", null);
    }

    // --- Helpers ---

    private User getCurrentUserOrThrow() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Current user not found"));
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
