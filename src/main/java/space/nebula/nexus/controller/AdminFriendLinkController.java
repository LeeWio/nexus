package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.FriendLinkStatus;
import space.nebula.nexus.payload.request.FriendLinkRequest;
import space.nebula.nexus.payload.response.FriendLinkResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.IFriendLinkService;

@RestController
@RequestMapping("/api/v1/admin/friend-links")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Friend Link Management", description = "Endpoints for managing and moderating friend links")
public class AdminFriendLinkController {

    private final IFriendLinkService friendLinkService;

    @GetMapping
    @Operation(summary = "Search all friend links (Management)")
    public ApiResponse<PageResult<FriendLinkResponse>> searchFriendLinks(@PageableDefault(size = 10) Pageable pageable) {
        return friendLinkService.retrieveAdminFriendLinks(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve friend link by ID")
    public ApiResponse<FriendLinkResponse> retrieveFriendLink(@PathVariable Long id) {
        return friendLinkService.retrieveFriendLinkById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new friend link directly")
    public ApiResponse<FriendLinkResponse> createFriendLink(@Valid @RequestBody FriendLinkRequest request) {
        return friendLinkService.createFriendLink(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update friend link details")
    public ApiResponse<FriendLinkResponse> updateFriendLink(@PathVariable Long id, @Valid @RequestBody FriendLinkRequest request) {
        return friendLinkService.updateFriendLink(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Moderate friend link application status")
    public ApiResponse<Void> moderateFriendLink(@PathVariable Long id, @RequestParam FriendLinkStatus status) {
        return friendLinkService.moderateFriendLink(id, status);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hard/Soft delete a friend link")
    public ApiResponse<Void> deleteFriendLink(@PathVariable Long id) {
        return friendLinkService.deleteFriendLink(id);
    }
}
