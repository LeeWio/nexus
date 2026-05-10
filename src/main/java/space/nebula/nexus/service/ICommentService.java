package space.nebula.nexus.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.PageResult;

public interface ICommentService {
    
    // Public
    ApiResponse<Void> submitComment(CommentRequest request, HttpServletRequest servletRequest);
    ApiResponse<PageResult<CommentResponse>> getPostComments(Long postId, Pageable pageable);
    ApiResponse<PageResult<CommentResponse>> getGuestbookComments(Pageable pageable);

    // Admin
    ApiResponse<PageResult<CommentResponse>> getAdminComments(Pageable pageable);
    ApiResponse<Void> updateCommentStatus(Long id, CommentStatus status);
    ApiResponse<Void> deleteComment(Long id);
}
