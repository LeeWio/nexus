package space.nebula.nexus.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
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
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.ICommentService;
import space.nebula.nexus.service.SensitiveWordService;
import space.nebula.nexus.utils.IpUtil;

/**
 * Professional implementation of ICommentService with hierarchical support, moderation,
 * sensitive word filtering, and async event notification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final SensitiveWordService sensitiveWordService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @LogOperation("Publish Comment")
    public ApiResponse<Void> publishComment(CommentRequest request, HttpServletRequest servletRequest) {
        // 1. Validate Target Post (if present)
        Post targetPost = null;
        if (request.postId() != null) {
            targetPost = postRepository.findById(request.postId())
                    .orElseThrow(() -> new ResourceNotFoundException("Post", "id", request.postId()));
            
            if (targetPost.getStatus() != PostStatus.PUBLISHED) {
                throw new BusinessException(403, "Cannot comment on unpublished posts");
            }
        }

        // 2. Sensitive word filtering & Auto-Moderation
        boolean containsViolationContent = sensitiveWordService.containsSensitiveWord(request.content());
        String sanitizedContent = sensitiveWordService.filter(request.content());

        // 3. Validate Parent Comment if exists
        Comment parentComment = null;
        if (request.parentId() != null) {
            parentComment = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.parentId()));
            
            // Validate that parent matches the same post context (or both are guestbook)
            boolean contextMatch = (targetPost == null && parentComment.getPost() == null) ||
                                 (targetPost != null && parentComment.getPost() != null && parentComment.getPost().getId().equals(targetPost.getId()));
            
            if (!contextMatch) {
                throw new BusinessException(400, "Parent comment belongs to a different context");
            }
        }

        // 4. Get Authenticated Author
        User commentAuthor = SecurityUtil.getCurrentUserOrThrow(userRepository);

        // 5. Build and Save Comment Entity
        Comment newComment = new Comment();
        newComment.setContent(sanitizedContent);
        newComment.setPost(targetPost);
        newComment.setUser(commentAuthor);
        newComment.setParent(parentComment);
        
        // Auto-reject if sensitive words detected
        if (containsViolationContent) {
            newComment.setStatus(CommentStatus.REJECTED);
            log.warn("Comment by {} rejected due to content violation: {}", commentAuthor.getUsername(), request.content());
        } else {
            newComment.setStatus(CommentStatus.PENDING);
        }

        newComment.setIpAddress(IpUtil.getIpAddress(servletRequest));
        newComment.setUserAgent(servletRequest.getHeader("User-Agent"));

        commentRepository.save(newComment);
        
        // 6. Publish async notification event
        eventPublisher.publishEvent(new CommentSubmittedEvent(this, newComment));

        if (containsViolationContent) {
            return ApiResponse.error(400, "Comment contains prohibited content and has been rejected.");
        }
        
        return ApiResponse.success("Comment submitted successfully and is awaiting moderation", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PageResult<CommentResponse>> retrieveCommentsByPost(Long postId, Pageable pageable) {
        Page<Comment> postComments = commentRepository.findAllByPostIdAndParentIsNullAndStatus(
                postId, CommentStatus.APPROVED, pageable);
        
        return ApiResponse.success(PageResult.of(postComments.map(commentMapper::toResponse)));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PageResult<CommentResponse>> retrieveGuestbookComments(Pageable pageable) {
        Page<Comment> guestbookComments = commentRepository.findAllByPostIsNullAndParentIsNullAndStatus(
                CommentStatus.APPROVED, pageable);
        
        return ApiResponse.success(PageResult.of(guestbookComments.map(commentMapper::toResponse)));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PageResult<CommentResponse>> searchCommentsForManagement(Pageable pageable) {
        Page<Comment> allComments = commentRepository.findAll(pageable);
        return ApiResponse.success(PageResult.of(allComments.map(commentMapper::toResponse)));
    }

    @Override
    @Transactional
    @LogOperation("Moderate Comment")
    public ApiResponse<Void> moderateComment(Long id, CommentStatus status) {
        Comment targetComment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
        
        targetComment.setStatus(status);
        commentRepository.save(targetComment);
        log.info("Comment {} moderated status updated to {}", id, status);
        return ApiResponse.success("Comment moderation completed successfully", null);
    }

    @Override
    @Transactional
    @LogOperation("Delete Comment")
    public ApiResponse<Void> deleteComment(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Comment", "id", id);
        }
        commentRepository.deleteById(id);
        log.info("Comment {} deleted from system", id);
        return ApiResponse.success("Comment deleted successfully", null);
    }
}
