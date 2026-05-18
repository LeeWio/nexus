package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.ICommentService;

@RestController
@RequestMapping("/api/v1/admin/comments")
@Tag(name = "Admin Comment API", description = "Endpoints for comment moderation and management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommentController {

    @Resource
    private ICommentService commentService;

    @GetMapping
    @Operation(summary = "Search all comments (Management)")
    public ApiResponse<PageResult<CommentResponse>> searchComments(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return commentService.searchCommentsForManagement(pageable);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Moderate a comment status")
    public ApiResponse<Void> moderateComment(
            @PathVariable Long id, 
            @RequestParam CommentStatus status) {
        return commentService.moderateComment(id, status);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hard delete a comment")
    public ApiResponse<Void> deleteComment(@PathVariable Long id) {
        return commentService.deleteComment(id);
    }
}
