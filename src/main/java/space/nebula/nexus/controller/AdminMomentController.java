package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.MomentRequest;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.IMomentService;

/**
 * Controller for managing micro-blog posts (Moments). Allows administrators to
 * create, update, and organize brief status updates.
 */
@Tag(name = "Admin Moment Management", description = "Endpoints for managing microblogs and status updates")
@RestController
@RequestMapping("/api/v1/admin/moments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMomentController
{

	private final IMomentService momentService;

	@GetMapping
	@Operation(summary = "Get all moments", description = "Retrieve a paginated list of all moments for management.")
	public ApiResponse<PageResult<MomentResponse>> getAllMoments(
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
	{
		return momentService.getAdminMoments(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get moment by ID", description = "Fetch details for a specific moment.")
	public ApiResponse<MomentResponse> getMomentById(@Parameter(description = "Moment ID") @PathVariable Long id)
	{
		return momentService.getMomentById(id);
	}

	@PostMapping
	@Operation(summary = "Create moment", description = "Post a new brief update or microblog.")
	public ApiResponse<MomentResponse> createMoment(@Valid @RequestBody MomentRequest request)
	{
		return momentService.createMoment(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update moment", description = "Modify the content or visibility of an existing moment.")
	public ApiResponse<MomentResponse> updateMoment(@Parameter(description = "Moment ID") @PathVariable Long id,
			@Valid @RequestBody MomentRequest request)
	{
		return momentService.updateMoment(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete moment", description = "Permanently remove a moment.")
	public ApiResponse<Void> deleteMoment(@Parameter(description = "Moment ID") @PathVariable Long id)
	{
		return momentService.deleteMoment(id);
	}
}
