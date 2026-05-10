package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.ICommentService;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/public/guestbook")
@RequiredArgsConstructor
@Tag(name = "Public Guestbook", description = "Public endpoints for the global guestbook")
public class PublicGuestbookController {

    private final ICommentService commentService;

    @GetMapping
    @Operation(summary = "Get guestbook comments", description = "Returns top-level approved comments for the guestbook")
    public ApiResponse<PageResult<CommentResponse>> getGuestbookComments(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return commentService.getGuestbookComments(pageable);
    }

    @PostMapping
    @Operation(summary = "Submit a guestbook comment")
    @RateLimit(count = 3, time = 10, unit = TimeUnit.MINUTES, message = "Guestbook frequency too high. Please wait.")
    public ApiResponse<Void> submitGuestbookComment(@Valid @RequestBody CommentRequest request, HttpServletRequest servletRequest) {
        // Ensure postId is null for guestbook
        CommentRequest guestbookRequest = new CommentRequest(request.content(), null, request.parentId());
        return commentService.submitComment(guestbookRequest, servletRequest);
    }
}
