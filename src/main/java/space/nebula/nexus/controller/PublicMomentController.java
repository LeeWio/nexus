package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.IMomentService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/public/moments")
@RequiredArgsConstructor
@Tag(name = "Public Moments", description = "Public endpoints for viewing and interacting with microblogs")
public class PublicMomentController {

	private final IMomentService momentService;

	@GetMapping
	@Operation(summary = "Get published moments timeline", description = "Retrieve the public microblog timeline in reverse chronological order.")
	public ApiResponse<PageResult<MomentResponse>> getPublicMoments(
			@Parameter(description = "Zero-based request pagination. Responses use a one-based page number.") @PageableDefault(size = 20) Pageable pageable) {
		return momentService.getPublicMoments(pageable);
	}

	@GetMapping("/liked")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Get liked moment IDs", description = "Returns which supplied moment IDs the current user has liked.")
	@SecurityRequirement(name = "bearerAuth")
	public ApiResponse<Set<Long>> getLikedMomentIds(
			@Parameter(description = "Moment IDs to check. Repeat the query parameter for multiple IDs.", example = "1") @RequestParam List<Long> ids) {
		return momentService.getLikedMomentIds(ids);
	}

	@PostMapping("/{id}/like")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Like a moment", description = "Add a like from the current user. Repeating the request does not create duplicate likes.")
	@SecurityRequirement(name = "bearerAuth")
	@RateLimit(count = 5, time = 1, unit = TimeUnit.MINUTES, message = "Too many likes. Please slow down.")
	public ApiResponse<Void> likeMoment(@Parameter(description = "Moment ID") @PathVariable Long id) {
		return momentService.likeMoment(id);
	}

	@DeleteMapping("/{id}/like")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Remove a moment like", description = "Remove the current user's like. Repeating the request is safe when the moment is already unliked.")
	@SecurityRequirement(name = "bearerAuth")
	public ApiResponse<Void> unlikeMoment(@Parameter(description = "Moment ID") @PathVariable Long id) {
		return momentService.unlikeMoment(id);
	}
}
