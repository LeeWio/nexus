package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.service.IInteractionService;

@Tag(name = "User Interactions", description = "Endpoints for social interactions (like, favorite)")
@RestController
@RequestMapping("/api/v1/public/interactions")
@RequiredArgsConstructor
public class PublicInteractionController {

    private final IInteractionService interactionService;

    @Operation(summary = "Like a post")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/posts/{postId}/like")
    public ApiResponse<Void> likePost(@PathVariable Long postId) {
        return interactionService.likePost(postId);
    }

    @Operation(summary = "Unlike a post")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/posts/{postId}/unlike")
    public ApiResponse<Void> unlikePost(@PathVariable Long postId) {
        return interactionService.unlikePost(postId);
    }

    @Operation(summary = "Favorite a post")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/posts/{postId}/favorite")
    public ApiResponse<Void> favoritePost(@PathVariable Long postId) {
        return interactionService.favoritePost(postId);
    }

    @Operation(summary = "Unfavorite a post")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/posts/{postId}/unfavorite")
    public ApiResponse<Void> unfavoritePost(@PathVariable Long postId) {
        return interactionService.unfavoritePost(postId);
    }
}
