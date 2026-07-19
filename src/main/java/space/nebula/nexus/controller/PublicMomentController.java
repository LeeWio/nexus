package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.IMomentService;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/public/moments")
@RequiredArgsConstructor
@Tag(name = "Public Moments", description = "Public endpoints for viewing and interacting with microblogs")
public class PublicMomentController
{

	private final IMomentService momentService;

	@GetMapping
	@Operation(summary = "Get published moments timeline")
	public ApiResponse<PageResult<MomentResponse>> getPublicMoments(@PageableDefault(size = 20) Pageable pageable)
	{
		return momentService.getPublicMoments(pageable);
	}

	@PostMapping("/{id}/like")
	@Operation(summary = "Like a moment (anonymous allowed with rate limit)")
	@RateLimit(count = 5, time = 1, unit = TimeUnit.MINUTES, message = "Too many likes. Please slow down.")
	public ApiResponse<Void> likeMoment(@PathVariable Long id)
	{
		return momentService.likeMoment(id);
	}
}
