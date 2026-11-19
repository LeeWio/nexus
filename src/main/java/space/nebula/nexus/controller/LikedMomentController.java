package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.IMomentService;

@RestController
@RequestMapping("/api/v1/user/moments/liked")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Moment Likes", description = "Private view of the signed-in account's likes")
public class LikedMomentController {

	private final IMomentService momentService;

	@GetMapping
	@Operation(summary = "List liked Moments", description = "Returns visible Moments the current account liked, newest like first.")
	public ApiResponse<PageResult<MomentResponse>> getLikedMoments(@PageableDefault(size = 12) Pageable pageable) {
		return momentService.getLikedMoments(pageable);
	}
}
