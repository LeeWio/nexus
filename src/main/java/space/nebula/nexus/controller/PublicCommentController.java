package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.ICommentService;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/public/comments")
@Tag(name = "Public Comment API", description = "Public endpoints for viewing and submitting comments")
public class PublicCommentController {

    @Resource
    private ICommentService commentService;

    @PostMapping
    @Operation(summary = "Publish a new comment (Requires Login)")
    @RateLimit(count = 5, time = 15, unit = TimeUnit.MINUTES, message = "Comment frequency too high. Please wait 15 minutes.")
    public ApiResponse<Void> publishComment(
            @Valid @RequestBody CommentRequest request, 
            HttpServletRequest servletRequest) {
        return commentService.publishComment(request, servletRequest);
    }

    @GetMapping("/post/{postId}")
    @Operation(summary = "Retrieve hierarchical comments for a post")
    public ApiResponse<PageResult<CommentResponse>> retrieveComments(
            @PathVariable Long postId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return commentService.retrieveCommentsByPost(postId, pageable);
    }
}
