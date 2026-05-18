package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.FriendLinkRequest;
import space.nebula.nexus.payload.response.FriendLinkResponse;
import space.nebula.nexus.service.IFriendLinkService;
import space.nebula.nexus.common.annotation.RateLimit;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/public/friend-links")
@RequiredArgsConstructor
@Tag(name = "Public Friend Links", description = "Public endpoints for exploring and applying for friend links")
public class PublicFriendLinkController {

    private final IFriendLinkService friendLinkService;

    @GetMapping
    @Operation(summary = "Retrieve all approved friend links")
    public ApiResponse<List<FriendLinkResponse>> retrieveFriendLinks() {
        return friendLinkService.retrievePublicFriendLinks();
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply for a new friend link exchange")
    @RateLimit(count = 5, time = 1, unit = TimeUnit.HOURS, message = "Application limit reached. Please try again later.")
    public ApiResponse<Void> applyForFriendLink(@Valid @RequestBody FriendLinkRequest request) {
        return friendLinkService.applyForFriendLink(request);
    }
}
