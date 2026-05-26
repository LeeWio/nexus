package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * Controller for administrative friend link management. Provides endpoints for
 * moderating external link applications and managing the blogroll.
 */
@Tag(name = "Admin Friend Link Management", description = "Endpoints for managing and moderating friend links and blogroll")
@RestController
@RequestMapping("/api/v1/admin/friend-links")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFriendLinkController
{

	private final IFriendLinkService friendLinkService;

	@GetMapping
	@Operation(summary = "Search all friend links", description = "Retrieve a paginated list of all friend link applications for moderation.")
	public ApiResponse<PageResult<FriendLinkResponse>> searchFriendLinks(
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 10) Pageable pageable)
	{
		return friendLinkService.retrieveAdminFriendLinks(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Retrieve friend link by ID", description = "Fetch detailed information for a specific friend link application.")
	public ApiResponse<FriendLinkResponse> retrieveFriendLink(
			@Parameter(description = "Friend Link ID") @PathVariable Long id)
	{
		return friendLinkService.retrieveFriendLinkById(id);
	}

	@PostMapping
	@Operation(summary = "Create friend link", description = "Manually add a new friend link to the blogroll.")
	public ApiResponse<FriendLinkResponse> createFriendLink(@Valid @RequestBody FriendLinkRequest request)
	{
		return friendLinkService.createFriendLink(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update friend link", description = "Modify an existing friend link's details, URL, or avatar.")
	public ApiResponse<FriendLinkResponse> updateFriendLink(
			@Parameter(description = "Friend Link ID") @PathVariable Long id,
			@Valid @RequestBody FriendLinkRequest request)
	{
		return friendLinkService.updateFriendLink(id, request);
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Moderate status", description = "Approve or reject a friend link application.")
	public ApiResponse<Void> moderateFriendLink(@Parameter(description = "Friend Link ID") @PathVariable Long id,
			@Parameter(description = "Target status (e.g., APPROVED, REJECTED)") @RequestParam FriendLinkStatus status)
	{
		return friendLinkService.moderateFriendLink(id, status);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete friend link", description = "Permanently remove a friend link from the system.")
	public ApiResponse<Void> deleteFriendLink(@Parameter(description = "Friend Link ID") @PathVariable Long id)
	{
		return friendLinkService.deleteFriendLink(id);
	}
}
