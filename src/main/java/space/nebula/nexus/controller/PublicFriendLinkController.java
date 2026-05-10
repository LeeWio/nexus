package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.FriendLinkResponse;
import space.nebula.nexus.service.IFriendLinkService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/friend-links")
@RequiredArgsConstructor
@Tag(name = "Public Friend Links", description = "Public endpoints for friend links")
public class PublicFriendLinkController {

    private final IFriendLinkService friendLinkService;

    @GetMapping
    @Operation(summary = "Get published friend links", description = "Returns all published friend links sorted by priority and date")
    public ApiResponse<List<FriendLinkResponse>> getPublicFriendLinks() {
        return friendLinkService.getPublicFriendLinks();
    }
}
